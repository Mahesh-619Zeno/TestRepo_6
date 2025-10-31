import java.util.Arrays;

public class BubbleSort {
    public static void main(String[] args) {
        int[] numbers = {5, 3, 8, 6, 2};
        for (int i = 0; i < numbers.length - 1; i++) {
            for (int j = 0; j < numbers.length - i - 1; j++) {
                if (numbers[j] > numbers[j+1]) {
                    int temp_value = numbers[j]; numbers[j] = numbers[j+1]; numbers[j+1] = temp_value;
                }
            }
        }
        System.out.println("Sorted: " + Arrays.toString(numbers));
    }
}
