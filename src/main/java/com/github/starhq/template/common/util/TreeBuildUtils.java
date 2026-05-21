package com.github.starhq.template.common.util;

import com.github.starhq.template.model.vo.tree.Tree;
import lombok.experimental.UtilityClass;

import java.util.*;

/**
 * Utility class for converting flat lists into hierarchical tree structures.
 *
 * <p>Uses a two-pass algorithm leveraging a {@link HashMap} to achieve O(N) time complexity,
 * which is significantly more efficient than recursive nested loops (O(N^2)) for large datasets.
 *
 * @author starhq
 */
@UtilityClass
public class TreeBuildUtils {

    /**
     * Builds a tree structure using the default root node identification strategy.
     *
     * <p>A node is considered a root if its {@code parentId} is either {@code null} or {@code 0L}.
     *
     * @param list the flat list of tree nodes
     * @param <T>  the concrete type of the tree node, must implement {@link Tree}
     * @return a list containing only the root nodes (with their descendants nested within)
     */
    public static <T extends Tree<T>> List<T> build(List<T> list) {
        return build(list, null);
    }

    /**
     * Builds a tree structure using a custom root node identifier.
     *
     * <p><b>Algorithm Steps:</b>
     * <ol>
     *   <li><b>Indexing Pass:</b> Iterates through the flat list, storing all nodes in a Map
     *       keyed by their ID. Crucially, it forcefully initializes the {@code children} list
     *       for every node to prevent {@link NullPointerException} when adding children later.</li>
     *   <li><b>Assembly Pass:</b> Iterates again, looking up each node's parent in the Map.
     *       If a parent is found, the current node is added to the parent's children list.
     *       If no parent is found (or matches the root criteria), it is added to the root list.</li>
     * </ol>
     *
     * @param list   the flat list of tree nodes
     * @param rootId the specific ID that defines a root node. If {@code null}, defaults to checking for {@code null} or {@code 0L}.
     * @param <T>    the concrete type of the tree node
     * @return a list of root nodes with fully assembled child hierarchies
     */
    public static <T extends Tree<T>> List<T> build(List<T> list,
                                                    Long rootId) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        // Capacity set to list.size() prevents unnecessary HashMap rehashing
        Map<Long, T> nodeMap = new HashMap<>(list.size());
        List<T> roots = new ArrayList<>();

        // 1. First loop: O(N) indexing and defensive initialization
        for (T node : list) {
            nodeMap.put(node.getId(), node);
            // ✅ Defense: Guarantee that leaf nodes have an empty list instead of null.
            // This is critical because JSON serializers (like Jackson) require non-null collections
            // to correctly render "children: []" instead of omitting the field entirely.
            node.setChildren(new ArrayList<>());
        }

        // 2. Second loop: O(N) parent-child assembly
        for (T node : list) {
            Long parentId = node.getParentId();

            // Determine if the current node qualifies as a root
            boolean isRoot = (rootId == null)
                    ? (parentId == null || parentId == 0L)
                    : rootId.equals(parentId);

            if (isRoot) {
                roots.add(node);
            } else {
                T parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    // Safe to retrieve and add, as we guaranteed initialization in the first loop
                    parentNode.getChildren().add(node);
                }
                // Note: If parentNode is null, it means the data contains an "orphan" node
                // (a node pointing to a non-existent parent). We silently discard it to prevent
                // structural corruption of the tree.
            }
        }

        return roots;
    }
}
