package org.cobbzilla.util.collection;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class CombinationsGenerator {

    // The main function that gets all combinations of size n-1 to 1, in set of size n.
    // This function mainly uses combinationUtil()
    public static Set<Set<String>> generateCombinations(Set<String> elements) {
        Set<Set<String>> result = new LinkedHashSet<>();

        // i - A number of elements which will be used in the combination in this iteration
        for (int i = elements.size() - 1; i >= 1 ; i--) {
            // A temporary array to store all combinations one by one
            String[] data = new String[i];
            // Get all combination using temporary array 'data'
            result = _generate(result, elements.toArray(new String[elements.size()]),
                               data, 0, elements.size() - 1, 0, i);
        }
        return result;
    }

    /**
     * @param combinations - Resulting array with all combinations of arr
     * @param arr - Input Array
     * @param data - Temporary array to store current combination
     * @param start - Staring index in arr[]
     * @param end - Ending index in arr[]
     * @param index - Current index in data[]
     * @param r - Size of a combination
     */
    private static Set<Set<String>> _generate(Set<Set<String>> combinations, String[] arr,
                                              String[] data, int start, int end, int index, int r) {
        // Current combination is ready
        if (index == r) {
            Set<String> current = new HashSet<>();
            for (int j = 0; j < r; j++) {
                current.add(data[j]);
            }
            combinations.add(current);
            return combinations;
        }

        // replace index with all possible elements. The condition `end - i + 1 >= r - index` makes sure that including
        // one element at index will make a combination with remaining elements at remaining positions
        for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
            data[index] = arr[i];
            combinations = _generate(combinations, arr, data, i + 1, end, index + 1, r);
        }

        return combinations;
    }
}
