package gitlet;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Collections;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Andrew Tom
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if ("init".equals(args[0])) {
            init();
            return;
        }
        if (!gitlet.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        switch (args[0]) {
        case "add":
            if (args.length > 2) {
                System.out.println("Incorrect operands.");
                return;
            }
            add(args[1]);
            break;
        case "rm":
            if (args.length > 2) {
                System.out.println("Incorrect operands.");
                return;
            }
            remove(args[1]);
            break;
        case "commit":
            if (args.length > 2) {
                System.out.println("Incorrect operands.");
                return;
            }
            makeCommit(args[1]); break;
        case "log":
            getLog();
            break;
        case "global-log":
            getFullLogs();
            break;
        case "find":
            if (args.length > 2) {
                System.out.println("Incorrect operands.");
                return;
            }
            finder(args[1]);
            break;
        case "status":
            myStatus();
            break;
        case "branch":
            if (args.length > 2) {
                System.out.println("Incorrect operands.");
                return;
            }
            brancher(args[1]); break;
        default:
            switchPart2(args);
        }
    }

    /**
     * Second part of Switch statement.
     * @param args The args passed into main.
     * @throws IOException
     */
    static void switchPart2(String... args) throws IOException {
        switch (args[0]) {
        case "checkout":
            if (args.length == 3) {
                if (!args[1].equals("--")) {
                    System.out.println("Incorrect operands.");
                }
                checkoutCase1(args[2]);
            } else if (args.length == 4) {
                if (!args[2].equals("--")) {
                    System.out.println("Incorrect operands.");
                }
                checkoutCase2(args[1], args[3]);
            } else {
                checkoutCase3(args[1]);
            }
            break;
        case "rm-branch":
            if (args.length > 2) {
                System.out.println("Incorrect operands.");
                return;
            }
            rmBrancher(args[1]);
            break;
        case "reset":
            if (args.length > 2) {
                System.out.println("Incorrect operands.");
                return;
            }
            resetter(args[1]);
            break;
        case "merge":
            merge(args[1], null);
            break;
        case "add-remote":
            addRm(args[1], args[2]);
            break;
        case "rm-remote":
            removeRm(args[1]);
            break;
        case "push":
            pusher(args[1], args[2]);
            break;
        case "fetch":
            fetcher(args[1], args[2]);
            break;
        case "pull":
            puller(args[1], args[2]);
            break;
        default:
            System.out.println("No command with that name exists.");
        }
    }

    /**
     * File var for CWD.
     */
    private static File cwd = new File(System.getProperty("user.dir"));
    /**
     * File var for the .gitlet dir.
     */
    private static File gitlet = new File(".gitlet");
    /**
     * File var for the add Stage.
     */
    private static File addStage = new File(".gitlet/addStage");
    /**
     * File var for the remove Stage.
     */
    private static File removeStage = new File(".gitlet/removeStage");
    /**
     * File var for the storing area for blobs.
     */
    private static File blobStorage = new File(".gitlet/blobby");
    /**
     * File var for the storing area for commits.
     */
    private static File commitStorage = new File(".gitlet/commits");
    /**
     * File var for the storing area of pointers.
     */
    private static File headPointers = new File(".gitlet/headPointers");
    /**
     * File var for the storing area of remote repo info.
     */
    private static File remoteRepos = new File(".gitlet/remoteRepos");
    /**
     * File var for the commits that are fetched.
     */
    private static File fetchedCommits = new File(".gitlet/fetchedCommits");


    /**
     * I create all the folders I need inside .gitlet.
     * I also create the first commit. This first commit
     * becomes the pointer which is located with
     * FILE POINTER.
     * The first commit is also added to the commit folder.
     */
    public static void init() {
        if (!gitlet.exists()) {
            gitlet.mkdir();
            blobStorage.mkdir();
            commitStorage.mkdir();
            addStage.mkdir();
            removeStage.mkdir();
            fetchedCommits.mkdir();

            FatMap branchPointers = new FatMap();

            Commit firstCommit = new Commit(null, "initial commit");

            firstCommit.setID();

            branchPointers.put("current", "master");
            branchPointers.put("master", firstCommit.getID());

            Utils.writeObject(headPointers, branchPointers);

            Commit.writeCommit(firstCommit.getID(), firstCommit);

            FatMap remoteMap = new FatMap();

            Utils.writeObject(remoteRepos, remoteMap);


        } else {
            System.out.println("A Gitlet version-control "
                    + "system already exists in the current directory.");
        }
    }

    /**
     * First thing that happens is that the filePath is
     * converted into a file. Then, if the file
     * exists, it is converted into bytes. These bytes are
     * then stored using its SHA-1 hash inside the
     * .gitlet/blobby folder.
     * @param filePath The filePath for the file to be added.
     */
    public static void add(String filePath) {
        Commit curCommit = Commit.grabCurrentCommit();

        File addFile = new File(filePath);
        if (addFile.exists()) {

            byte[] savedFile = Utils.readContents(addFile);
            String hash = Utils.sha1((Object) savedFile);

            List<String> removeStageList = Utils.plainFilenamesIn(removeStage);


            if (Commit.doNotAdd(filePath, curCommit, hash)) {

                List<String> addStageList = Utils.plainFilenamesIn(addStage);

                if (addStageList.contains(filePath)) {
                    File f = new File(addStage.getPath() + "/" + filePath);
                    f.delete();
                }
                if (removeStageList.contains(filePath)) {
                    File f = new File(removeStage.getPath() + "/" + filePath);
                    f.delete();
                }
                return;
            }

            if (removeStageList.contains(filePath)) {
                File f = new File(removeStage.getPath() + "/" + filePath);
                f.delete();
            }

            Utils.writeContents(new File(addStage.getPath()
                    + "/" + filePath), (Object) savedFile);

        } else {
            System.out.println("File does not exist.");
        }
    }

    /**
     * First, the file that is passed in is checked to see if
     * it exists. If it does exist, it will delete it.
     * If it does not exist, then it will move on to condition two.
     * This will check if the file is currently being
     * tracked by the current commit. If it is, then the file is
     * un-tracked. Otherwise, nothing happens and the
     * program will exit printing an error message.
     * @param filePath The file that is trying to be removed.
     */
    public static void remove(String filePath) throws IOException {
        File checkStage = new File(addStage.getPath() + "/" + filePath);
        if (checkStage.exists()) {
            checkStage.delete();
            return;
        }

        Commit curCommit = Commit.grabCurrentCommit();
        if (curCommit.getFileNameToHash().containsKey(filePath)) {
            File deleteCWDFile = new File(cwd.getPath() + "/" + filePath);
            if (deleteCWDFile.exists()) {
                deleteCWDFile.delete();
            }

            File f = new File(removeStage.getPath() + "/" + filePath);
            f.createNewFile();
        } else {
            System.out.println("No reason to remove the file.");
        }
    }

    /**
     * First, all files in the staging area are listed. Then,
     * each file in the staging area is added to the commit
     * HashMap and written into the blobStorage folder. Then, we
     * iterate through the tracked files in the current
     * commit. If any of said files have NOT been placed into this
     * new commit already (essentially being overridden),
     * then these new files will be added to the new commit. After
     * all this is done, the entire staging area is cleared.
     * Then, the current branches pointer is updated in the HashMap,
     * and it is rewritten for updating.
     * @param message The message to go with this new commit.
     */
    public static void makeCommit(String message) {
        if (message.length() == 0) {
            System.out.println("Please enter a commit message.");
        }

        if (addStage.list().length == 0 && removeStage.list().length == 0) {
            System.out.println("No changes added to the commit.");
            return;
        }

        Commit parent = Commit.grabCurrentCommit();
        Commit newCommit;

        newCommit = new Commit(parent.getID(), message);

        Commit.addStagingFilesToCommit(newCommit);

        Commit.addParentFilesToCommit(newCommit, parent);

        newCommit.setID();

        String branchName = Commit.grabCurrentBranch();
        Commit.updatePointers(branchName, newCommit.getID());

        newCommit.setBranch(branchName);

        parent.getBabiesBranch().add(newCommit.getBranch());
        Commit.writeCommit(parent.getID(), parent);

        Commit.writeCommit(newCommit.getID(), newCommit);
    }

    /**
     * Builds a commit for merges who will have two parents.
     * @param message The message to go with this new commit.
     * @param otherParent The second parent to be tracked.
     */
    public static void makeCommit(String message, String otherParent) {
        Commit parent = Commit.grabCurrentCommit();
        Commit other = Commit.retrieve(otherParent);
        Commit newCommit;

        newCommit = new Commit(parent.getID() + " " + otherParent, message);

        Commit.addStagingFilesToCommit(newCommit);
        Commit.addParentFilesToCommit(newCommit, parent);

        newCommit.setID();

        String branchName = Commit.grabCurrentBranch();
        Commit.updatePointers(branchName, newCommit.getID());

        newCommit.setBranch(branchName);

        parent.getBabiesBranch().add(newCommit.getBranch());
        other.getBabiesBranch().add(newCommit.getBranch());
        Commit.writeCommit(parent.getID(), parent);
        Commit.writeCommit(other.getID(), other);

        Commit.writeCommit(newCommit.getID(), newCommit);
    }

    /**
     * The method for printing out the log of commits.
     */
    public static void getLog() {
        for (Commit cur = Commit.grabCurrentCommit();;
             cur = Commit.grabFirstParent(cur)) {
            System.out.println("===");
            System.out.println("commit " + cur.getID());
            System.out.println("Date: " + cur.getMyDate());
            System.out.println(cur.getMsg());
            if (cur.getParents().get(0) == null) {
                break;
            }
            System.out.println();
        }
    }

    /**
     * The method for printing global logs.
     */
    public static void getFullLogs() {
        String[] commits = commitStorage.list();
        for (String id : commits) {
            Commit cur = Commit.retrieve(id);
            System.out.println("===");
            System.out.println("commit " + id);
            System.out.println("Date: " + cur.getMyDate());
            System.out.println(cur.getMsg());
            System.out.println();
        }
    }

    /**
     * The method for finding a commit with a specific msg.
     * @param message The message to be searched for.
     */
    public static void finder(String message) {
        String[] commits = commitStorage.list();
        boolean foundOne = false;
        for (String id : commits) {
            Commit cur = Commit.retrieve(id);
            if (cur.getMsg().equals(message)) {
                System.out.println(id);
                foundOne = true;
            }
        }
        if (!foundOne) {
            System.out.println("Found no commit with that message.");
        }
    }

    /**
     * The method that displays the current status.
     */
    public static void myStatus() {
        ArrayList<String> branchArr = Commit.grabAllBranches();
        System.out.println("=== Branches ===");
        System.out.println("*" + branchArr.get(0));
        branchArr.remove(0); Collections.sort(branchArr);
        for (int x = 0; x < branchArr.size(); x++) {
            System.out.println(branchArr.get(x));
        }
        System.out.println(); System.out.println("=== Staged Files ===");
        List<String> addLst = Utils.plainFilenamesIn(addStage);
        for (String file : addLst) {
            System.out.println(file);
        }
        System.out.println(); System.out.println("=== Removed Files ===");
        List<String> removeLst = Utils.plainFilenamesIn(removeStage);
        for (String file : removeLst) {
            System.out.println(file);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> immutableList = Utils.plainFilenamesIn(cwd);
        ArrayList<String> cwdArr = new ArrayList<>(immutableList);
        Commit currentCommit = Commit.grabCurrentCommit();
        HashMap<String, String> nameHashMap = currentCommit.getFileNameToHash();
        Set<String> trackedFiles = nameHashMap.keySet();
        ArrayList<String> trackedList = new ArrayList<>(trackedFiles);
        trackedList.addAll(addLst); Collections.sort(trackedList);
        for (String file : trackedList) {
            if (cwdArr.contains(file)) {
                if (!removeLst.contains(file)) {
                    byte[] fileByte = Utils.readContents(new File(file));
                    String fileHash = Utils.sha1((Object) fileByte);
                    if (addLst.contains(file)) {
                        byte[] addByte = Utils.readContents(new
                                File(addStage.getPath() + "/" + file));
                        String addHash = Utils.sha1(addByte);
                        if (!addHash.equals(fileHash)) {
                            System.out.println(file + " (modified)");
                        }
                    } else {
                        if (!fileHash.equals(nameHashMap.get(file))) {
                            System.out.println(file + " (modified)");
                        }
                    }
                }
                if (removeLst.contains(file)) {
                    continue;
                }
                cwdArr.remove(file);
            } else if (!removeLst.contains(file)) {
                System.out.println(file + " (deleted)");
            }
        }
        System.out.println(); System.out.println("=== Untracked Files ===");
        for (String untracked : cwdArr) {
            if (!new File(untracked).isDirectory()) {
                System.out.println(untracked);
            }
        }
    }

    /**
     * Method for checkout case 1.
     * @param fileName The String of the file to be checked out.
     */
    public static void checkoutCase1(String fileName) {
        Commit current = Commit.grabCurrentCommit();
        checkoutCase2(current.getID(), fileName);
    }

    /**
     * The method for checkout case 2.
     * @param id The String id of the commit.
     * @param fileName The String of the file to be checked out.
     */
    public static void checkoutCase2(String id, String fileName) {

        Commit specifiedCommit = Commit.retrieve(id);
        if (specifiedCommit == null) {
            return;
        }

        HashMap<String, String> nameToBlob =
                specifiedCommit.getFileNameToHash();

        String blobHash = "";

        if (!nameToBlob.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        } else {
            blobHash = nameToBlob.get(fileName);
        }

        byte[] blobByte = Commit.retrieveBlob(blobHash);

        File blobLocation = new File(cwd.getPath() + "/" + fileName);
        Utils.writeContents(blobLocation, blobByte);
    }

    /**
     * The method for checkout case 3.
     * @param branchName String of the branch to checkout.
     */
    public static void checkoutCase3(String branchName) {
        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);

        String currentBranch = pointerMap.get("current");

        if (branchName.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        String currentHeadID = pointerMap.get(currentBranch);
        Commit currentHead = Commit.retrieve(currentHeadID);
        String newID = pointerMap.get(branchName);
        if (newID == null) {
            System.out.println("No such branch exists.");
            return;
        }
        Commit newHead = Commit.retrieve(newID);
        List<String> untrackedFilesImmutable = Utils.plainFilenamesIn(cwd);
        ArrayList<String> untrackedFiles =
                new ArrayList<>(untrackedFilesImmutable);
        for (int x = 0; x < untrackedFiles.size(); x++) {
            String file = untrackedFiles.get(x);
            if (currentHead.getFileNameToHash().containsKey(file)) {
                untrackedFiles.remove(file);
                x--;
            }
        }
        for (String untracked : untrackedFiles) {
            if (newHead.getFileNameToHash().keySet().contains(untracked)) {
                System.out.println("There is an untracked file in "
                        + "the way; delete it, or add and commit it first.");
                return;
            }
        }
        for (String trackedFile : newHead.getFileNameToHash().keySet()) {
            String trackedFileID = newHead.getFileNameToHash().get(trackedFile);
            File trackedCWD = new File(cwd.getPath() + "/" + trackedFile);

            File trackedStorage = new
                    File(blobStorage.getPath() + "/" + trackedFileID);

            byte[] trackedFileByte = Utils.readContents(trackedStorage);

            Utils.writeContents(trackedCWD, (Object) trackedFileByte);
        }


        for (String oldTracked : currentHead.getFileNameToHash().keySet()) {
            if (!newHead.getFileNameToHash().keySet().contains(oldTracked)) {
                File deleteOld = new File(cwd + "/" + oldTracked);
                deleteOld.delete();
            }
        }

        Commit.clearStage();

        pointerMap.put("current", branchName);

        Utils.writeObject(headPointers, pointerMap);
    }

    /**
     * Grabs the pointer HashMap from storage, and inserts a new branch inside.
     * Also will create a split point serialized file for tracking purposes
     * with merge.
     * This split point file is simply a String containing the ID of the
     * commit where a split happened.
     * @param name The name of the branch to create.
     */
    public static void brancher(String name) {
        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);

        if (pointerMap.containsKey(name)) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        String currentBranch = pointerMap.get("current");
        String currentHeadID = pointerMap.get(currentBranch);
        pointerMap.put(name, currentHeadID);

        Utils.writeObject(headPointers, pointerMap);

    }

    /**
     * The method to remove a branch.
     * @param name String of the branch to remove.
     */
    public static void rmBrancher(String name) {
        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);
        if (pointerMap.get("current").equals(name)) {
            System.out.println("Cannot remove the current branch.");
        } else if (!pointerMap.containsKey(name)) {
            System.out.println("A branch with that name does not exist.");
        }

        pointerMap.remove(name);
        Utils.writeObject(headPointers, pointerMap);
    }

    /**
     * Method that resets the head to a specific commit.
     * @param id The ID of the commit to reset to.
     */
    public static void resetter(String id) {
        Commit newHead = Commit.retrieve(id);
        if (newHead == null) {
            return;
        }
        HashMap<String, String> nameToBlob = newHead.getFileNameToHash();

        Commit currentCommit = Commit.grabCurrentCommit();
        HashMap<String, String> currentTracked =
                currentCommit.getFileNameToHash();

        List<String> untrackedFilesImmutable = Utils.plainFilenamesIn(cwd);
        ArrayList<String> untrackedFiles =
                new ArrayList<>(untrackedFilesImmutable);
        for (int x = 0; x < untrackedFiles.size(); x++) {

            String file = untrackedFiles.get(x);

            if (currentCommit.getFileNameToHash().containsKey(file)) {
                untrackedFiles.remove(file);
                x--;
            }
        }
        for (String untracked : untrackedFiles) {
            if (newHead.getFileNameToHash().keySet().contains(untracked)) {
                System.out.println("There is an untracked file in the way; "
                        +
                        "delete it, or add and commit it first.");
                return;
            }
        }


        for (String blob : nameToBlob.keySet()) {
            File storedBlob = new
                    File(blobStorage.getPath() + "/" + nameToBlob.get(blob));
            File blobLocation = new File(cwd.getPath() + "/" + blob);

            byte[] blobByte = Utils.readContents(storedBlob);

            Utils.writeContents(blobLocation, blobByte);
        }

        for (String blob : currentTracked.keySet()) {
            if (!newHead.getFileNameToHash().containsKey(blob)) {
                File delete = new File(cwd.getPath() + "/" + blob);
                delete.delete();
            }
        }

        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);

        String currentBranch = pointerMap.get("current");
        pointerMap.put(currentBranch, id);

        Utils.writeObject(headPointers, pointerMap);
        Commit.clearStage();
    }

    /**
     * Helper Function to print Merge errors.
     * @param branchName The name of the branch being merged into.
     * @param otherBranchCommit The Commit from the other branch.
     * @return Boolean of whether or not to return and short circuit
     * in the main merge method.
     */
    public static boolean mergeErr(String branchName,
                                     Commit otherBranchCommit) {
        if (addStage.list().length > 0 || removeStage.list().length > 0) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (branchName.equals(Commit.grabCurrentBranch())) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return otherBranchCommit == null;
    }

    /**
     * Helper for merge to find the splitPoint.
     * @param givenSplit If not null, this is the splitPoint.
     * @param currentBranchCommit The commit of the current Branch.
     * @param otherBranchCommit The commit of the other Branch.
     * @param branchName The name of the branch being merged into.
     * @return The splitPoint commit.
     */
    public static Commit
        splitPointHelper(Commit givenSplit, Commit currentBranchCommit,
        Commit otherBranchCommit, String branchName) {
        Commit splitPoint = null;
        if (givenSplit != null) {
            return givenSplit;
        }
        Commit cur = currentBranchCommit;
        ArrayList<String> bothBranches = new ArrayList<>();
        bothBranches.add(currentBranchCommit.getBranch());
        bothBranches.add(branchName);
        while (true) {
            ArrayList<String> babies = cur.getBabiesBranch();
            if (babies.size() >= 2) {
                boolean commitContainedBabies = true;
                for (String branch : bothBranches) {
                    if (!babies.contains(branch)) {
                        commitContainedBabies = false;
                    }
                }
                if (commitContainedBabies) {
                    splitPoint = cur; break;
                } else {
                    Commit c = Commit.grabFirstParent(otherBranchCommit);
                    while (true) {
                        if (c.getID().equals(cur.getID())) {
                            splitPoint = cur;
                        }
                        if (c.getParents().get(0) == null) {
                            break;
                        }
                        c = Commit.grabFirstParent(c);
                    }
                }
            }
            ArrayList<String> parents = cur.getParents();
            if (parents.size() == 2) {
                String secondParentID = parents.get(1);
                Commit checkSecondParent = Commit.retrieve(secondParentID);
                babies = checkSecondParent.getBabiesBranch();
                boolean commitContainedBabies = true;
                for (String branch : bothBranches) {
                    if (!babies.contains(branch)) {
                        commitContainedBabies = false;
                    }
                }
                if (commitContainedBabies) {
                    splitPoint = checkSecondParent; break;
                }
            }
            if (parents.get(0) == null) {
                break;
            }
            cur = Commit.retrieve(parents.get(0));
        }
        if (splitPointHelper2(splitPoint, currentBranchCommit, branchName)) {
            return null;
        }
        return splitPoint;
    }

    /**
     *
     * @param splitPoint The splitPoint that is checked.
     * @param currentBranchCommit The current branch Commit.
     * @param branchName The name of the branch to be examined.
     * @return True or false on whether to return null
     * for splitPointHelper.
     */
    public static boolean splitPointHelper2(Commit splitPoint,
                                 Commit currentBranchCommit,
                                 String branchName) {
        if (splitPoint == null) {
            if (currentBranchCommit.getBranch().equals(branchName)) {
                System.out.println("Current branch fast-forwarded.");
                checkoutCase3(branchName);
            } else {
                System.out.println("Given branch is an "
                        + "ancestor of the current branch.");
            }
            return true;
        }
        return false;
    }

    /**
     * The method that merges two branches together.
     * Creates a new commit out of the two branches.
     * @param branchName The branch to merge into.
     * @param givenSplit Null if no split Point is predetermined.
     *                   Otherwise, we can use this for our split.
     * @throws IOException
     */
    public static void merge(String branchName,
                             Commit givenSplit) throws IOException {
        Commit currentBranchCommit = Commit.grabCurrentCommit();
        Commit otherBranchCommit = Commit.grabBranchCommit(branchName);
        if (mergeErr(branchName, otherBranchCommit)) {
            return;
        }



        Commit splitPoint = splitPointHelper(givenSplit,
                currentBranchCommit, otherBranchCommit, branchName);
        if (splitPoint == null) {
            return;
        }

        HashMap<String, String> splitNameToID =
                splitPoint.getFileNameToHash();
        HashMap<String, String> curNameToID =
                currentBranchCommit.getFileNameToHash();
        HashMap<String, String> otherNameToID =
                otherBranchCommit.getFileNameToHash();
        ArrayList<String> curFiles = new ArrayList<>(curNameToID.keySet());
        ArrayList<String> otherFiles = new ArrayList<>(otherNameToID.keySet());
        ArrayList<String> untrackedFiles = new
                ArrayList<>(Arrays.asList(cwd.list()));
        for (String file : curFiles) {
            untrackedFiles.remove(file);
        }
        for (String file : otherNameToID.keySet()) {
            if (untrackedFiles.contains(file)) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it, or add and commit it first.");
                return;
            }
        }

        mergeFor(splitNameToID, curNameToID, otherNameToID,
                curFiles, otherFiles);
        mergeHelperEnding(branchName, curFiles,
                otherFiles, curNameToID,
                otherNameToID, currentBranchCommit, otherBranchCommit);
    }

    /**
     * The for loop that handles all the actual merging. Enforces all
     * rules, and creates any merge conflict files. Will print out
     * any kind of merge conflict message as well.
     * @param splitNameToID The splitPoint id.
     * @param curNameToID The HashMap for current fileNames to IDs.
     * @param otherNameToID The HashMap for other fileNames to IDs.
     * @param curFiles The current file ArrayList.
     * @param otherFiles The other file ArrayList.
     * @throws IOException
     */
    public static void mergeFor(HashMap<String, String> splitNameToID,
                                HashMap<String, String> curNameToID,
                                HashMap<String, String> otherNameToID,
                                ArrayList<String> curFiles,
                                ArrayList<String> otherFiles)
            throws IOException {
        for (String fileCheck : splitNameToID.keySet()) {
            String originalID = splitNameToID.get(fileCheck);
            File locationOriginalFile = new
                    File(blobStorage.getPath() + "/" + originalID);
            byte[] originalByte = Utils.readContents(locationOriginalFile);
            String originalSHA = Utils.sha1(originalByte);
            if (curNameToID.containsKey(fileCheck)
                    && !otherNameToID.containsKey(fileCheck)) {

                firstIfCase(fileCheck, curNameToID, originalSHA);

            } else if (!curNameToID.containsKey(fileCheck)
                    && otherNameToID.containsKey(fileCheck)) {

                secondIfCase(fileCheck, otherNameToID, originalSHA);

            } else if (curNameToID.containsKey(fileCheck)
                    && otherNameToID.containsKey(fileCheck)) {
                String currentID = curNameToID.get(fileCheck);
                File locationCurFile = new
                        File(blobStorage.getPath() + "/" + currentID);
                byte[] curByte = Utils.readContents(locationCurFile);
                String curSHA = Utils.sha1((Object) curByte);
                String otherID = otherNameToID.get(fileCheck);
                File locationOtherFile = new
                        File(blobStorage.getPath() + "/" + otherID);
                byte[] otherByte = Utils.readContents(locationOtherFile);
                String otherSHA = Utils.sha1((Object) otherByte);
                if (originalSHA.equals(curSHA)
                        && !originalSHA.equals(otherSHA)) {
                    File writeFile = new File(fileCheck);
                    File stageToAdd = new
                            File(addStage.getPath() + "/" + fileCheck);
                    Utils.writeContents(writeFile, (Object) otherByte);
                    Utils.writeContents(stageToAdd, (Object) otherByte);
                } else if (!originalSHA.equals(curSHA)
                        && !originalSHA.equals(otherSHA)
                        && !curSHA.equals(otherSHA)) {
                    File writeFile = new File(fileCheck);
                    File stageAdd = new
                            File(addStage.getPath() + "/" + fileCheck);
                    String contents = "<<<<<<< HEAD\n";
                    String curContents =
                            Utils.readContentsAsString(locationCurFile);
                    String otherContents =
                            Utils.readContentsAsString(locationOtherFile);
                    contents += curContents
                            + "=======\n" + otherContents + ">>>>>>>\n";
                    Utils.writeContents(writeFile, contents);
                    Utils.writeContents(stageAdd, contents);
                    System.out.println("Encountered a merge conflict.");
                }
            }
            curFiles.remove(fileCheck);
            otherFiles.remove(fileCheck);
        }
    }

    /**
     * Handles the first If case in the MergeFor helper method.
     * This is the case in which the current Commit contains
     * the file we are checking.
     * @param fileCheck The name of the File being checked.
     * @param curNameToID The HashMap for current fileNames to IDs.
     * @param originalSHA The original Hash SHA-1 ID.
     * @throws IOException
     */
    public static void firstIfCase(String fileCheck,
                                   HashMap<String, String> curNameToID,
                                   String originalSHA)
            throws IOException {
        String currentID = curNameToID.get(fileCheck);
        File locationCurFile = new
                File(blobStorage.getPath() + "/" + currentID);
        byte[] curByte = Utils.readContents(locationCurFile);
        String curSHA = Utils.sha1(curByte);
        if (curSHA.equals(originalSHA)) {
            File deleteCWD = new File(fileCheck);
            File locationUntrack = new
                    File(removeStage.getPath() + "/" + fileCheck);
            deleteCWD.delete();
            locationUntrack.createNewFile();
        } else {
            File writeFile = new File(fileCheck);
            File stageAdd = new
                    File(addStage.getPath() + "/" + fileCheck);
            String contents = "<<<<<<< HEAD\n";
            String curContents =
                    Utils.readContentsAsString(locationCurFile);
            contents += curContents + "=======\n" + ">>>>>>>\n";
            Utils.writeContents(writeFile, contents);
            Utils.writeContents(stageAdd, contents);
            System.out.println("Encountered a merge conflict.");
        }
    }

    /**
     * If case in the MergeFor helper method.
     * This is the case in which the current Commit does not
     * contain the file we are checking.
     * @param fileCheck The file we are currently checking.
     * @param otherNameToID The HashMap of other fileNames to IDs.
     * @param originalSHA The original SHA-1 Hash ID.
     */
    public static void secondIfCase(String fileCheck,
                                    HashMap<String, String> otherNameToID,
                                    String originalSHA) {
        String otherID = otherNameToID.get(fileCheck);
        File locationOtherFile = new
                File(blobStorage.getPath() + "/" + otherID);
        byte[] otherByte = Utils.readContents(locationOtherFile);
        String otherSHA = Utils.sha1((Object) otherByte);
        if (!otherSHA.equals(originalSHA)) {
            File writeFile = new File(fileCheck);
            File stageAdd = new
                    File(addStage.getPath() + "/" + fileCheck);
            String contents = "<<<<<<< HEAD\n";
            contents += "=======\n" + ">>>>>>>\n";
            Utils.writeContents(writeFile, contents);
            Utils.writeContents(stageAdd, contents);
            System.out.println("Encountered a merge conflict.");
        }
    }

    /**
     * Helper for merge in the case fileCheck is present in both
     * other and current.
     * @param branchName The branch name to be used.
     * @param curFiles The ArrayList of current Files.
     * @param otherFiles The ArrayList of other Files.
     * @param curNameToID The HashMap of current Names to IDs.
     * @param otherNameToID The HashMap of other Names to IDs.
     * @param currentBranchCommit The current branch Commit.
     * @param otherBranchCommit The other branch Commit.
     */
    public static void mergeHelperEnding(String branchName,
                                   ArrayList<String> curFiles,
                                   ArrayList<String> otherFiles,
                                   HashMap<String,
                                           String> curNameToID,
                                   HashMap<String, String> otherNameToID,
                                   Commit currentBranchCommit,
                                   Commit otherBranchCommit) {
        ArrayList<String> absentList = new ArrayList<>(curFiles);
        absentList.addAll(otherFiles);
        HashSet<String> absentSet = new HashSet<>(absentList);
        for (String fileCheck : absentSet) {
            if (!curNameToID.containsKey(fileCheck)
                    && otherNameToID.containsKey(fileCheck)) {
                String otherID = otherNameToID.get(fileCheck);
                File locationOtherFile = new
                        File(blobStorage.getPath() + "/" + otherID);
                byte[] otherByte = Utils.readContents(locationOtherFile);
                File writeFile = new File(fileCheck);
                File stageToAdd = new
                        File(addStage.getPath() + "/" + fileCheck);
                Utils.writeContents(writeFile, (Object) otherByte);
                Utils.writeContents(stageToAdd, (Object) otherByte);
            } else if (curNameToID.containsKey(fileCheck)
                    && otherNameToID.containsKey(fileCheck)) {
                String currentID = curNameToID.get(fileCheck);
                File locationCurFile = new
                        File(blobStorage.getPath() + "/" + currentID);
                String otherID = otherNameToID.get(fileCheck);
                File locationOtherFile = new
                        File(blobStorage.getPath() + "/" + otherID);
                File writeFile = new File(fileCheck);
                File stageAdd = new
                        File(addStage.getPath() + "/" + fileCheck);
                String contents = "<<<<<<< HEAD\n";
                String curContents =
                        Utils.readContentsAsString(locationCurFile);
                String otherContents =
                        Utils.readContentsAsString(locationOtherFile);
                contents += curContents
                        + "=======\n" + otherContents + ">>>>>>>\n";
                Utils.writeContents(writeFile, contents);
                Utils.writeContents(stageAdd, contents);
                System.out.println("Encountered a merge conflict.");
            }
        }
        makeCommit("Merged " + branchName
                + " into " + currentBranchCommit.getBranch()
                + ".", otherBranchCommit.getID());
    }



    /**
     * Remembers this remote repository by adding to the remoteRepos HashMap.
     * @param remoteName The name of the remote Repo
     * @param remotePath The file path of the remote repo
     */
    public static void addRm(String remoteName, String remotePath) {
        FatMap remotesMap = Utils.readObject(remoteRepos, FatMap.class);
        if (remotesMap.containsKey(remoteName)) {
            System.out.println("A remote with that name already exists.");
        } else {
            remotesMap.put(remoteName, remotePath);
            Utils.writeObject(remoteRepos, remotesMap);
        }
    }

    /**
     * Opposite of add_rm. Simply removes a remote from the HashMap
     * @param remoteName The remote to be removed.
     */
    public static void removeRm(String remoteName) {
        FatMap remotesMap = Utils.readObject(remoteRepos, FatMap.class);
        if (!remotesMap.containsKey(remoteName)) {
            System.out.println("A remote with that name does not exist.");
        } else {
            remotesMap.remove(remoteName);
            Utils.writeObject(remoteRepos, remotesMap);
        }
    }

    /**
     * Appends the current branches commits to the end of the remote_Branch.
     * The command will only work if the remote branches head is in the history
     * of the local current branch
     * @param remoteName The name of the remote to be pushed to.
     * @param remoteBranchName The branch of the
     *                           remote that commits will be appended to.
     */
    public static void pusher(String remoteName, String remoteBranchName) {
        FatMap remotesMap = Utils.readObject(remoteRepos, FatMap.class);
        String remotePathString = remotesMap.get(remoteName);

        File remoteFileCheck = new File(remotePathString);
        if (!remoteFileCheck.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        File remoteBranchMapLocation = new
                File(remotePathString + "/headPointers");
        FatMap remoteBranchMap =
                Utils.readObject(remoteBranchMapLocation, FatMap.class);

        String remoteLatestCommitID =
                remoteBranchMap.get(remoteBranchName);
        ArrayList<Commit> changeList = new ArrayList<>();
        Commit checkCommit = Commit.grabCurrentCommit();
        String currentID = checkCommit.getID();
        for (;; checkCommit = Commit.grabFirstParent(checkCommit)) {
            String curID = checkCommit.getID();
            String parentID = checkCommit.getParents().get(0);
            if (curID.equals(remoteLatestCommitID)) {
                break;
            } else if (parentID == null) {
                System.out.println("Please pull down "
                        +
                        "remote changes before pushing.");
                return;
            } else {
                changeList.add(checkCommit);
            }
        }
        File remoteCommitDir = new File(remotePathString + "/commits");
        File remoteBlobDir = new File(remotePathString + "/blobby");
        for (Commit writeCommit : changeList) {
            HashMap<String, String> nameToBlob =
                    writeCommit.getFileNameToHash();
            for (String blobName : nameToBlob.keySet()) {
                String blobID = nameToBlob.get(blobName);
                File blobLocation = new
                        File(blobStorage.getPath() + "/" + blobID);
                byte[] blobByte = Utils.readContents(blobLocation);
                File f = new File(remoteBlobDir.getPath() + "/" + blobID);
                Utils.writeContents(f, blobByte);
            }
            File f = new
                    File(remoteCommitDir.getPath() + "/" + writeCommit.getID());
            Utils.writeObject(f, writeCommit);
        }
        String curRemoteBranch = remoteBranchMap.get("current");
        if (!curRemoteBranch.equals(remoteBranchName)) {
            remoteBranchMap.put("current", remoteBranchName);
        }
        remoteBranchMap.put(remoteBranchName, currentID);
        Utils.writeObject(remoteBranchMapLocation, remoteBranchMap);
    }

    /**
     * Will grab all commits that haven't yet been
     * seen, and places them in a branch called
     * [remote name]/[remote branch name]. If this
     * branch did not previously exist, then it will
     * be created now.
     * @param remoteName Name of the remote to be fetched from.
     * @param remoteBranchName The branch that
     * is being fetched from in the remote.
     * @return The commit that the fetcher splits at.
     */
    public static Commit fetcher(String remoteName,
                                 String remoteBranchName) {
        FatMap remotesMap = Utils.readObject(remoteRepos, FatMap.class);
        String remotePathString = remotesMap.get(remoteName);
        File remoteHeadPointers = new File(remotePathString + "/headPointers");
        if (!remoteHeadPointers.exists()) {
            System.out.println("Remote directory not found.");
            return null;
        }
        File remoteCommits = new File(remotePathString + "/commits");
        File remoteBlobs = new File(remotePathString + "/blobby");
        Commit curCommit = Commit.grabCurrentCommit();
        String curCommitID = curCommit.getID();
        ArrayList<Commit> changeList = new ArrayList<>(); FatMap remoteBranches
                = Utils.readObject(remoteHeadPointers, FatMap.class);
        String latestRemoteCommitID = remoteBranches.get(remoteBranchName);
        if (latestRemoteCommitID == null) {
            System.out.println("That remote does not have that branch.");
            return null;
        }
        File latestRemoteCommitLocation = new File(remotePathString
                + "/commits/" + latestRemoteCommitID);
        Commit checkCommit =
                Utils.readObject(latestRemoteCommitLocation, Commit.class);
        if (Utils.plainFilenamesIn(commitStorage).
                contains(checkCommit.getID())) {
            return null;
        }
        while (checkCommit.getParents().get(0) != null
                && !checkCommit.getID().equals(curCommitID)) {
            changeList.add(checkCommit);
            HashMap<String, String> blobNameToHash =
                    checkCommit.getFileNameToHash();
            for (String blobName : blobNameToHash.keySet()) {
                String blobID = blobNameToHash.get(blobName);
                File remoteBlobLocation = new
                        File(remoteBlobs.getPath() + "/" + blobID);
                File localBlobLocation = new
                        File(blobStorage.getPath() + "/" + blobID);
                byte[] blobByte = Utils.readContents(remoteBlobLocation);
                Utils.writeContents(localBlobLocation, (Object) blobByte);
            }
            String parentID = checkCommit.getParents().get(0);
            File parentLocation =
                    new File(remoteCommits.getPath() + "/" + parentID);
            checkCommit = Utils.readObject(parentLocation, Commit.class);
        }
        for (Commit c : changeList) {
            File writeCommit =
                    new File(commitStorage.getPath() + "/" + c.getID());
            Utils.writeObject(writeCommit, c);
        }
        String savedRemoteBranch = remoteName + "/" + remoteBranchName;
        FatMap localBranches = Utils.readObject(headPointers, FatMap.class);
        if (!localBranches.containsKey(savedRemoteBranch)) {
            localBranches.put(remoteName + "/" + remoteBranchName, "");
        }
        localBranches.put(savedRemoteBranch, latestRemoteCommitID);
        Utils.writeObject(headPointers, localBranches);
        return curCommit;
    }

    /**
     *
     * @param remoteName Name of the remote to be fetched from.
     * @param remoteBranchName The branch that is being
     *                           pulled from in the remote.
     */
    public static void puller(String remoteName,
                              String remoteBranchName) throws IOException {
        Commit split = fetcher(remoteName, remoteBranchName);
        merge(remoteName + "/" + remoteBranchName, split);
    }
}
