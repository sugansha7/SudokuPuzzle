import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Subsets {
    private final Set<Integer> set;
    private final int subSetSize;
    private final Map<Set<Integer>, Integer> subsets;

    public Subsets(Set<Integer> set, int subSetSize) {
        this.set = set;
        this.subSetSize = subSetSize;
        subsets = new HashMap<Set<Integer>, Integer>();
    }

    public static void main(String[] args) {
        Set<Integer> set = new TreeSet<Integer>();
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        Subsets subsets = new Subsets(set, 3);
        for (Set<Integer> subset : subsets.getSubSets()) {
            System.out.println(subset);
        }
    }

    public Set<Set<Integer>> getSubSets() {
        Integer[] arr = new Integer[set.size()];
        set.toArray(arr);
        permute(arr, 0);
        return subsets.keySet();
    }

    private void permute(Integer[] arr, int fixedIndex) {
        addToCombination(arr, fixedIndex);
        for (int i = fixedIndex + 1; i < arr.length; ++i) {
            swap(arr, fixedIndex, i);
            permute(arr, fixedIndex + 1);
            swap(arr, i, fixedIndex); // backtrack the swap
        }
    }

    private void swap(Integer[] arr, int fixedIndex, int i) {
        int tmp = arr[fixedIndex];
        arr[fixedIndex] = arr[i];
        arr[i] = tmp;
    }

    private void addToCombination(Integer[] arr, int fixedIndex) {
        Set<Integer> combo = new TreeSet<Integer>();
        for (int i = 0; i < subSetSize; ++i) {
            combo.add(arr[i]);
        }
        if (!subsets.containsKey(combo)) {
            subsets.put(combo, 0);
        }
    }
}
