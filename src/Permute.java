public class Permute {
    public static void permute(int[] list, int fixedIndex) {
        print(list);
        for (int i = fixedIndex + 1; i < list.length; ++i) {
            swap(list, i, fixedIndex);
            permute(list, fixedIndex + 1);
            swap(list, i, fixedIndex); // backtrack the swap
        }
    }

    private static void print(int[] list) {
        StringBuffer strBuf = new StringBuffer();
        for (int i : list) {
            strBuf.append(i);
        }
        System.out.println(strBuf);
    }

    private static void swap(int[] list, int varIndex, int fixedIndex) {
        int tmp = list[varIndex];
        list[varIndex] = list[fixedIndex];
        list[fixedIndex] = tmp;
    }

    public static void main(String[] args) {
        int[] arr = {1, 2, 3, 4, 5};
        permute(arr, 0);
    }
}
