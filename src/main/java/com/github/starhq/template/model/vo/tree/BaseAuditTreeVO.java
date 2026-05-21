package com.github.starhq.template.model.vo.tree;

import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.util.List;

/**
 * Abstract base view object for hierarchical tree structures with audit trail capabilities.
 * <p>
 * This class extends {@link BaseAuditVO} to inherit comprehensive audit fields
 * ({@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt})
 * and provides generic tree capabilities ({@code parentId}, {@code children})
 * for recursive hierarchy representation with full audit context.
 * <p>
 * Designed as a reusable foundation for menu trees, category trees, organization charts,
 * and other nested data structures where creation/modification history tracking is required.
 * <p>
 * <strong>Generic Type Parameter:</strong>
 * <p>
 * The type parameter {@code <T>} represents the concrete VO class that extends this base.
 * This enables type-safe recursive operations:
 * <pre>
 * {@code
 * // Example: Menu tree VO with audit fields
 * public class MenuAuditVO extends BaseAuditTreeVO<MenuAuditVO> {
 *     private String name;
 *     private String url;
 *     private String icon;
 *     // ... other menu-specific fields
 *     // Inherits: id, parentId, children, createdBy, createdAt, updatedBy, updatedAt
 * }
 *
 * // Usage: Type-safe tree operations with audit context
 * List<MenuAuditVO> tree = TreeUtils.buildTree(flatList, 0L);
 * MenuAuditVO rootNode = tree.get(0);
 * Long creatorId = rootNode.getCreatedBy(); // Audit field from BaseAuditVO
 * List<MenuAuditVO> children = rootNode.getChildren(); // Type-safe, no cast needed
 * }
 * </pre>
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Navigation Menus</strong>: Render hierarchical sidebar menus with audit trail for compliance</li>
 *     <li><strong>Category Trees</strong>: Display product categories with creation/modification history</li>
 *     <li><strong>Organization Charts</strong>: Visualize company structure with change tracking for HR audits</li>
 *     <li><strong>Permission Trees</strong>: Show role-based access control hierarchies with full audit context</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Reusability</strong>: Abstract base class eliminates duplication across tree-based VOs</li>
 *     <li><strong>Audit Integration</strong>: Inherits audit fields from {@link BaseAuditVO} for compliance tracking</li>
 *     <li><strong>Type Safety</strong>: Generic parameter {@code <T>} enables compile-time type checking for children</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 * </ul>
 * <p>
 * <strong>Serialization Strategy:</strong>
 * <p>
 * The {@code parentId} field uses {@code @JsonSerialize(using = ToStringSerializer.class)}
 * to convert {@link Long} to {@code String} in JSON output. This prevents precision loss
 * when consuming APIs from JavaScript/TypeScript clients (which use 53-bit integers).
 *
 * @param <T> the concrete VO type that extends this base (e.g., {@code MenuAuditVO extends BaseAuditTreeVO<MenuAuditVO>})
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-04
 * @see BaseAuditVO
 * @see Tree
 * @see com.github.starhq.template.common.util.TreeBuildUtils
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseAuditTreeVO<T> extends BaseAuditVO {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 1103929040429395290L;

    /**
     * The unique identifier of the parent node in the tree hierarchy.
     * <p>
     * Establishes the parent-child relationship: {@code Node 1..* ChildNode}.
     * Root nodes typically have {@code parentId = null} or {@code 0L} depending
     * on database design convention.
     * <p>
     * <strong>Serialization Strategy:</strong>
     * <p>
     * Annotated with {@code @JsonSerialize(using = ToStringSerializer.class)} to
     * convert the {@link Long} value to a {@code String} in JSON output. This prevents
     * precision loss when the API is consumed by JavaScript/TypeScript clients, which
     * represent integers as 64-bit floats with only 53 bits of precision.
     * <pre>
     * {@code
     * // Without ToStringSerializer:
     * { "parentId": 9007199254740993 }  // May be truncated in JS
     *
     * // With ToStringSerializer:
     * { "parentId": "9007199254740993" }  // Safe string representation
     * }
     * </pre>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references parent node's {@code id} for hierarchy integrity</li>
     *     <li>Nullability: {@code null} or {@code 0L} indicates root-level node (no parent)</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_parent_id ON table_name(parent_id)} for efficient tree queries</li>
     * </ul>
     * <p>
     * <strong>Tree Building Pattern:</strong>
     * <pre>
     * {@code
     * // Build tree from flat list using parent-child relationships
     * List<T> flatList = fetchFlatListFromDatabase();
     * List<T> tree = TreeUtils.buildTree(flatList, 0L); // 0L = root parent ID
     *
     * // Frontend: Render recursive tree component with audit info
     * <Tree :data="tree" :props="{ label: 'name', children: 'children' }">
     *   <template #node="{ node }">
     *     <div>
     *       <span>{{ node.name }}</span>
     *       <small class="text-gray-400">
     *         Created by: {{ node.createdBy }} at {{ node.createdAt }}
     *       </small>
     *     </div>
     *   </template>
     * </Tree>
     * }
     * </pre>
     *
     * @see ToStringSerializer
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">JavaScript Number.MAX_SAFE_INTEGER</a>
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /**
     * The list of child nodes in the tree hierarchy.
     * <p>
     * This field enables recursive tree traversal and rendering. Each element
     * in the list is an instance of the concrete type {@code <T>} that extends
     * this base class, ensuring type-safe access to child-specific properties.
     * <p>
     * <strong>Null-Safety Contract:</strong>
     * <ul>
     *     <li>Should never be {@code null} in well-formed responses; return empty list {@code []} if no children exist</li>
     *     <li>Frontend should handle empty lists gracefully (e.g., hide expand icon for leaf nodes)</li>
     *     <li>Backend tree-building utilities should initialize this field to {@code new ArrayList<>()} by default</li>
     * </ul>
     * <p>
     * <strong>Recursive Operations with Audit Context:</strong>
     * <pre>
     * {@code
     * // Example: Count total nodes in tree (including descendants) with audit filter
     * public int countNodesByCreator(BaseAuditTreeVO<?> node, Long targetCreatorId) {
     *     int count = node.getCreatedBy().equals(targetCreatorId) ? 1 : 0;
     *     if (node.getChildren() != null) {
     *         for (BaseAuditTreeVO<?> child : node.getChildren()) {
     *             count += countNodesByCreator(child, targetCreatorId); // Recursive count
     *         }
     *     }
     *     return count;
     * }
     *
     * // Example: Find recently modified subtree
     * public <T extends BaseAuditTreeVO<T>> List<T> findRecentSubtree(T root, OffsetDateTime cutoff) {
     *     List<T> recent = new ArrayList<>();
     *     if (root.getUpdatedAt().isAfter(cutoff)) {
     *         recent.add(root);
     *     }
     *     if (root.getChildren() != null) {
     *         for (T child : root.getChildren()) {
     *             recent.addAll(findRecentSubtree(child, cutoff)); // Recursive search
     *         }
     *     }
     *     return recent;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Recursive tree component with audit info display
     * <template #default="{ node }">
     *   <div class="tree-node">
     *     <div class="node-header">
     *       <span>{{ node.name }}</span>
     *       <a-tooltip :title="`Created by ${node.createdBy} at ${node.createdAt}`">
     *         <a-icon type="info-circle" class="ml-2 text-gray-400" />
     *       </a-tooltip>
     *     </div>
     *     <!-- Recursively render children -->
     *     <recursive-tree v-if="node.children?.length" :nodes="node.children" />
     *   </div>
     * </template>
     *
     * // React: Tree rendering with audit metadata
     * const AuditTreeNode = ({ node }) => (
     *   <div className="tree-node">
     *     <div className="node-header">
     *       <span>{node.name}</span>
     *       <Tooltip title={`Created by ${node.createdBy} at ${node.createdAt}`}>
     *         <Icon type="info" className="ml-2 text-gray-400" />
     *       </Tooltip>
     *     </div>
     *     {node.children?.map(child => (
     *       <AuditTreeNode key={child.id} node={child} />
     *     ))}
     *   </div>
     * );
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For deep trees (>1000 nodes), consider lazy-loading children via API on expand</li>
     *     <li>Avoid including unused audit fields in child VOs to minimize JSON payload size</li>
     *     <li>Cache tree structures by root ID for repeated access with invalidation on changes</li>
     *     <li>Use database-level recursive queries (CTE) for very large trees to reduce application memory pressure</li>
     * </ul>
     *
     * @see Tree
     * @see com.github.starhq.template.common.util.TreeBuildUtils#build(List, Long)
     */
    private List<T> children;

}