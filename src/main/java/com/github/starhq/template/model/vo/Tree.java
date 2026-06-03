package com.github.starhq.template.model.vo;

import java.util.List;

/**
 * Generic contract for hierarchical tree node representations.
 * <p>
 * This interface defines the minimal API that any tree-capable view object must implement
 * to participate in recursive tree operations such as building, traversing, filtering,
 * and rendering. It enables type-safe, reusable tree utilities across different domain models
 * (menus, categories, organizations, permissions, etc.).
 * <p>
 * <strong>Generic Type Parameter:</strong>
 * <p>
 * The type parameter {@code <T>} represents the concrete implementation class.
 * This self-referential generic pattern (also known as F-bounded polymorphism)
 * enables type-safe recursive operations without casting:
 * <pre>
 * {@code
 * // Example: Menu tree VO implementing Tree<MenuTreeVO>
 * public class MenuTreeVO extends BaseIdTreeVO<MenuTreeVO> implements Tree<MenuTreeVO> {
 *     private String name;
 *     private String url;
 *     // getId(), getParentId(), getChildren() inherited from base class
 * }
 *
 * // Usage: Type-safe tree utilities
 * List<MenuTreeVO> tree = TreeUtils.buildTree(flatList, 0L);
 * MenuTreeVO rootNode = tree.get(0);
 * List<MenuTreeVO> children = rootNode.getChildren(); // No cast needed
 * }
 * </pre>
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Tree Building</strong>: Convert flat lists to hierarchical structures via {@code TreeUtils.buildTree()}</li>
 *     <li><strong>Recursive Traversal</strong>: Implement depth-first/breadth-first searches with type-safe child access</li>
 *     <li><strong>Frontend Rendering</strong>: Provide structured data for Vue/React recursive tree components</li>
 *     <li><strong>Generic Utilities</strong>: Enable reusable tree operations (filter, sort, count, find) across domains</li>
 * </ul>
 * <p>
 * <strong>Implementation Guidelines:</strong>
 * <ul>
 *     <li><strong>Null-Safety</strong>: {@code getChildren()} should return empty list {@code []} instead of {@code null} for leaf nodes</li>
 *     <li><strong>Immutability</strong>: Consider returning unmodifiable lists from {@code getChildren()} if tree is read-only</li>
 *     <li><strong>Serialization</strong>: Ensure implementations are {@link java.io.Serializable} if used in distributed caches</li>
 *     <li><strong>Equality</strong>: Override {@code equals()} and {@code hashCode()} based on {@code id} for reliable tree operations</li>
 * </ul>
 *
 * @param <T> the concrete tree node type (self-referential generic, e.g., {@code MenuTreeVO implements Tree<MenuTreeVO>})
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-19
 * @see BaseIdTreeVO
 * @see BaseAuditTreeVO
 * @see com.github.starhq.template.common.util.TreeBuildUtils
 */
public interface Tree<T> {

    /**
     * Returns the unique identifier of this tree node.
     * <p>
     * This ID is used for:
     * <ul>
     *     <li>Building parent-child relationships during tree construction</li>
     *     <li>Referencing nodes in frontend components (e.g., {@code key} prop in Vue/React)</li>
     *     <li>Lookup operations in tree traversal algorithms</li>
     * </ul>
     * <p>
     * <strong>Contract Requirements:</strong>
     * <ul>
     *     <li>Must be globally unique within the tree scope</li>
     *     <li>Should not be {@code null} for persisted nodes</li>
     *     <li>Recommended type: {@link Long} for database sequence compatibility</li>
     * </ul>
     * <p>
     * <strong>Implementation Example:</strong>
     * <pre>
     * {@code
     * @Override
     * public Long getId() {
     *     return this.id; // From BaseIdVO or similar base class
     * }
     * }
     * </pre>
     *
     * @return the unique node identifier, or {@code null} for transient/unpersisted nodes
     */
    Long getId();

    /**
     * Sets the unique identifier of this tree node.
     * <p>
     * Typically used during object construction or deserialization.
     * For immutable tree nodes, this method may throw {@link UnsupportedOperationException}.
     *
     * @param id the unique node identifier to set
     */
    void setId(Long id);

    /**
     * Returns the unique identifier of the parent node in the tree hierarchy.
     * <p>
     * This value establishes the parent-child relationship:
     * <ul>
     *     <li>{@code null} or {@code 0L}: Indicates a root-level node (no parent)</li>
     *     <li>Positive {@code Long}: References the {@code id} of the parent node</li>
     * </ul>
     * <p>
     * <strong>Tree Building Usage:</strong>
     * <pre>
     * {@code
     * // TreeUtils uses getParentId() to link children to parents
     * for (T node : flatList) {
     *     Long parentId = node.getParentId();
     *     if (parentId == null || parentId.equals(rootId)) {
     *         tree.add(node); // Root node
     *     } else {
     *         T parent = nodeMap.get(parentId);
     *         if (parent != null) {
     *             parent.getChildren().add(node); // Link child to parent
     *         }
     *     }
     * }
     * }
     * </pre>
     * <p>
     * <strong>Implementation Example:</strong>
     * <pre>
     * {@code
     * @Override
     * public Long getParentId() {
     *     return this.parentId; // From BaseIdTreeVO or similar
     * }
     * }
     * </pre>
     *
     * @return the parent node's ID, or {@code null}/{@code 0L} if this is a root node
     */
    Long getParentId();

    /**
     * Sets the unique identifier of the parent node in the tree hierarchy.
     * <p>
     * Typically used during object construction or when re-parenting nodes.
     * For immutable tree structures, this method may throw {@link UnsupportedOperationException}.
     *
     * @param parentId the parent node's ID to set, or {@code null}/{@code 0L} for root nodes
     */
    void setParentId(Long parentId);

    /**
     * Returns the list of child nodes in the tree hierarchy.
     * <p>
     * This field enables recursive tree traversal and rendering. Each element
     * in the list is an instance of type {@code <T>}, ensuring type-safe access
     * to child-specific properties without casting.
     * <p>
     * <strong>Null-Safety Contract:</strong>
     * <ul>
     *     <li>Should <strong>never return {@code null}</strong> in well-formed implementations</li>
     *     <li>Return empty list {@code Collections.emptyList()} for leaf nodes (no children)</li>
     *     <li>Frontend components should handle empty lists gracefully (hide expand icon)</li>
     * </ul>
     * <p>
     * <strong>Recursive Traversal Example:</strong>
     * <pre>
     * {@code
     * // Depth-first search: Find node by ID
     * public <T extends Tree<T>> T findNodeById(T root, Long targetId) {
     *     if (root.getId().equals(targetId)) return root;
     *
     *     for (T child : root.getChildren()) { // Type-safe iteration
     *         T found = findNodeById(child, targetId);
     *         if (found != null) return found;
     *     }
     *     return null;
     * }
     *
     * // Count total nodes in subtree
     * public <T extends Tree<T>> int countNodes(T node) {
     *     int count = 1;
     *     for (T child : node.getChildren()) {
     *         count += countNodes(child); // Recursive count
     *     }
     *     return count;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Frontend Integration Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Recursive tree component
     * <template #default="{ node }">
     *   <div>
     *     <span>{{ node.name }}</span>
     *     <!-- Recursively render children -->
     *     <recursive-tree v-if="node.children?.length" :nodes="node.children" />
     *   </div>
     * </template>
     *
     * // React: Tree rendering with children mapping
     * const TreeNode = ({ node }) => (
     *   <div>
     *     <span>{node.name}</span>
     *     {node.children?.map(child => (
     *       <TreeNode key={child.id} node={child} />
     *     ))}
     *   </div>
     * );
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For deep trees (>1000 nodes), consider lazy-loading children via API on expand</li>
     *     <li>Avoid including unused fields in child VOs to minimize JSON payload size</li>
     *     <li>Cache tree structures by root ID for repeated access with invalidation on changes</li>
     * </ul>
     *
     * @return the list of child nodes; never {@code null} (empty list for leaf nodes)
     */
    List<T> getChildren();

    /**
     * Sets the list of child nodes in the tree hierarchy.
     * <p>
     * Typically used during tree construction or when dynamically modifying tree structure.
     * For immutable tree nodes, this method may throw {@link UnsupportedOperationException}.
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Initialize to {@code new ArrayList<>()} in constructors to ensure non-null</li>
     *     <li>Consider returning unmodifiable lists from {@code getChildren()} if tree is read-only</li>
     *     <li>When setting children, ensure no circular references (node cannot be its own descendant)</li>
     * </ul>
     *
     * @param children the list of child nodes to set; should not be {@code null}
     */
    void setChildren(List<T> children);

}