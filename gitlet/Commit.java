package gitlet;
import java.io.Serializable;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Date;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

/**
 * The data structure commit which stores multiple kinds of metadata.
 * @author Andrew Tom
 */
public class Commit implements Serializable {

    /**
     * The HashMap of files mapped to Hash IDs.
     */
    private HashMap<String, String> fileNameToHash = new HashMap<>();

    /**
     * The instance variable of a commit's msg.
     */
    private String msg;

    /**
     * The instance variable of a commit's id.
     */
    private String id;

    /**
     * An ArrayList of Strings showing the parents
     * of a commit.
     */
    private ArrayList<String> parents = new ArrayList<>();

    /**
     * An ArrayList of Strings showing the branch of
     * a commit's babies branches.
     */
    private ArrayList<String> babiesBranch = new ArrayList<>();

    /**
     * The instance variable for a commit's date.
     */
    private String myDate;

    /**
     * The instance variable for a commit's branch.
     */
    private String branch;

    /**
     * Constructor for commit class.
     * @param parent The parent(s) of the commit in an ArrayList<String>.
     * @param message The String message to be included in the msg
     *                instance variable.
     */
    Commit(String parent, String message) {

        setID();
        msg = message;
        if (parent == null) {
            parents.add(null);
            myDate = "Wed Dec 31 17:00:00 1969 -0700";
            return;
        }
        Date d = new Date();
        SimpleDateFormat f = new SimpleDateFormat("E MMM d hh:mm:ss yyyy Z");
        myDate = f.format(d);


        String[] parentArr = parent.split(" ");
        parents.addAll(Arrays.asList(parentArr));
    }

    /**
     * The getter method for id instance var.
     * @return The String id.
     */
    public String getID() {
        return id;
    }

    /**
     * The getter method for the fileNameToHash var.
     * @return HashMap of Strings mapped to strings.
     */
    public HashMap<String, String> getFileNameToHash() {
        return fileNameToHash;
    }

    /**
     * The getter method for the stored date var.
     * @return String of myDate.
     */
    public String getMyDate() {
        return myDate;
    }

    /**
     * Getter method for the parents ArrayList.
     * @return The ArrayList of parents.
     */
    public ArrayList<String> getParents() {
        return parents;
    }

    /**
     * The getter method of the babiesBranch instance var.
     * @return ArrayList containing babies branches.
     */
    public ArrayList<String> getBabiesBranch() {
        return babiesBranch;
    }

    /**
     * Getter method for the branch instance var.
     * @return String of the commit's branch.
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Setter method of the branch instance var.
     * @param branchName The name for the branch to be set to.
     */
    public void setBranch(String branchName) {
        branch = branchName;
    }

    /**
     * The getter method of the msg instance var.
     * @return The String msg that is stored by this commit.
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Grabs a blob from storage.
     * @param id The id of the blob.
     * @return The byte[] form of a blob.
     */
    public static byte[] retrieveBlob(String id) {
        File blobStorage = new File(".gitlet/blobby/" + id);
        return Utils.readContents(blobStorage);
    }

    /**
     * Adds all parent/tracked files to the commit
     * which are not staged for removal.
     * @param newCommit The new commit being created.
     * @param parent The parent commit to bring tracked files from.
     */
    public static void addParentFilesToCommit(Commit newCommit, Commit parent) {
        File removeStage = new File(".gitlet/removeStage");
        String[] removeLst = removeStage.list();
        ArrayList<String> checker = new ArrayList<>(Arrays.asList(removeLst));

        for (String str : parent.fileNameToHash.keySet()) {
            if (checker.contains(str)) {
                File del = new File(removeStage.getPath() + "/" + str);
                del.delete();
                continue;
            }
            if (!newCommit.fileNameToHash.containsKey(str)) {
                String hash = parent.fileNameToHash.get(str);
                newCommit.fileNameToHash.put(str, hash);
            }
        }
    }

    /**
     * Grabs the FIRST parent of the specified commit,
     * ignoring second parents from merges.
     * @param c Commit to grab the parent of
     * @return Returns the parent commit of param c
     */
    public static Commit grabFirstParent(Commit c) {
        return retrieve(c.parents.get(0));
    }

    /**
     * Adds all files from the staging area to the commit.
     * @param newCommit The commit that is being created.
     */
    public static void addStagingFilesToCommit(Commit newCommit) {
        File addStage = new File(".gitlet/addStage");
        File blobStorage = new File(".gitlet/blobby");
        String[] added = addStage.list();

        for (String str : added) {

            File curObjLocation = new File(addStage.getPath() + "/" + str);

            byte[] curObj = Utils.readContents(curObjLocation);

            String hashBlob = Utils.sha1((Object) curObj);

            File serialized = new File(blobStorage.getPath() + "/" + hashBlob);

            Utils.writeContents(serialized, (Object) curObj);

            newCommit.fileNameToHash.put(str, hashBlob);

            curObjLocation.delete();
        }
    }

    /**
     * Sets the ID of a commit.
     */
    public void setID() {
        byte[] commitByte = Utils.serialize(this);
        String hashID = Utils.sha1((Object) commitByte);
        this.id = hashID;
    }

    /**
     * Helper function for makeCommit. Checks whether a file should be
     * added or kept.
     * If the current commit already contains it with no differences,
     * then no reason to add it.
     * @param filePath The filePath to the blob.
     * @param curCommit The current Commit being compared to.
     * @param addFileHash The hash ID of the file for comparison of contents.
     * @return A boolean of whether said blob should be added or not.
     */
    public static boolean doNotAdd(String filePath, Commit curCommit,
                                   String addFileHash) {
        if (curCommit.fileNameToHash.containsKey(filePath)) {
            String compare = curCommit.fileNameToHash.get(filePath);
            if (compare.equals(addFileHash)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a commit to storage.
     * @param id The ID of the commit to be written.
     * @param newCommit The object commit to be written.
     */
    public static void writeCommit(String id, Commit newCommit) {
        File commitStorage = new File(".gitlet/commits");
        File writeCommitLocation = new File(commitStorage.getPath() + "/" + id);
        Utils.writeObject(writeCommitLocation, newCommit);
    }

    /**
     * Update a pointer in the pointer Map.
     * @param branchName The branch that is being updated.
     * @param hashCommit The ID to be inserted.
     */
    public static void updatePointers(String branchName, String hashCommit) {
        File headPointers = new File(".gitlet/headPointers");
        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);

        pointerMap.put(branchName, hashCommit);

        Utils.writeObject(headPointers, pointerMap);
    }

    /**
     * Grabs the name of the current branch.
     * @return Name of the current branch.
     */
    public static String grabCurrentBranch() {
        File headPointers = new File(".gitlet/headPointers");
        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);
        String curBranch = pointerMap.get("current");
        return curBranch;
    }

    /**
     * Grabs an ArrayList<String> of all branches.
     * @return Returns the list of all branches.
     */
    public static ArrayList<String> grabAllBranches() {
        File headPointers = new File(".gitlet/headPointers");
        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);
        ArrayList<String> result = new ArrayList<>();
        String current = pointerMap.get("current");
        result.add(current);
        for (String key : pointerMap.keySet()) {
            if (!key.equals(current) && !key.equals("current")) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * Clears the entire stage, both add and remove.
     */
    public static void clearStage() {
        File addStage = new File(".gitlet/addStage");
        File removeStage = new File(".gitlet/removeStage");
        for (String file : addStage.list()) {
            File deleteAdd = new File(addStage.getPath() + "/" + file);
            deleteAdd.delete();
        }
        for (String file : removeStage.list()) {
            File deleteRemove = new File(removeStage.getPath() + "/" + file);
            deleteRemove.delete();
        }
    }

    /**
     * Grabs the latest commit on the current branch.
     * @return Returns the commit on the current branch.
     */
    public static Commit grabCurrentCommit() {
        File headPointers = new File(".gitlet/headPointers");
        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);
        String curBranch = pointerMap.get("current");
        String id = pointerMap.get(curBranch);
        Commit currentCommit = retrieve(id);
        return currentCommit;
    }

    /**
     * Grabs the latest commit on the given branch.
     * @param branch The branch that the commit is on.
     * @return The latest commit of the specified branch.
     */
    public static Commit grabBranchCommit(String branch) {
        File headPointers = new File(".gitlet/headPointers");
        FatMap pointerMap = Utils.readObject(headPointers, FatMap.class);
        if (pointerMap.containsKey(branch)) {
            String branchID = pointerMap.get(branch);
            return retrieve(branchID);
        }

        System.out.println("A branch with that name does not exist.");
        return null;

    }

    /**
     * Grabs a commit from storage.
     * @param id The ID hash of the commit to retrieve.
     * @return Returns the commit whose hash is ID.
     */
    public static Commit retrieve(String id) {
        final int idSize = 40;
        if (id.length() < idSize) {
            File commitStorage = new File(".gitlet/commits");
            for (String commitID : commitStorage.list()) {
                if (commitID.contains(id)) {
                    File storedCommit = new File(".gitlet/commits/" + commitID);
                    Commit c = Utils.readObject(storedCommit, Commit.class);
                    return c;
                }
            }
            System.out.println("No commit with that id exists.");
            return null;
        }

        File storedCommit = new File(".gitlet/commits/" + id);
        if (!storedCommit.exists()) {
            System.out.println("No commit with that id exists.");
            return null;
        }
        Commit c = Utils.readObject(storedCommit, Commit.class);
        return c;
    }
}
