package com.github.starhq.template.common.util;

import com.github.starhq.template.model.vo.tree.Tree;
import lombok.experimental.UtilityClass;

import java.util.*;

/**
 * 通用树形结构组装工具类
 */
@UtilityClass
public class TreeBuildUtils {

    /**
     * 构建树形结构（默认根节点标识为 null 或 0）
     */
    public static <T extends Tree<T>> List<T> build(List<T> list) {
        return build(list, null);
    }

    /**
     * 构建树形结构（自定义根节点标识）
     */
    public static <T extends Tree<T>> List<T> build(List<T> list,
                                                    Long rootId) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, T> nodeMap = new HashMap<>(list.size());
        List<T> roots = new ArrayList<>();

        // 1. 第一层循环：将所有节点存入 Map，并强制初始化 children
        for (T node : list) {
            nodeMap.put(node.getId(), node);
            // ✅ 关键：保证所有节点都有 children 集合
            node.setChildren(new ArrayList<>());
        }

        // 2. 第二层循环：建立父子关系
        for (T node : list) {
            Long parentId = node.getParentId();

            boolean isRoot = (rootId == null)
                    ? (parentId == null || parentId == 0L)
                    : rootId.equals(parentId);

            if (isRoot) {
                roots.add(node);
            } else {
                T parentNode = nodeMap.get(parentId);
                if (parentNode != null) {
                    // ✅ 极简写法：既然上面初始化过了，直接强转拿过来 add 即可！
                    // 因为我们知道 T 肯定是 BaseAuditTreeVO 的子类，所以强转绝对安全
                    List<T> parentChildren = parentNode.getChildren();

                    parentChildren.add(node);
                }
            }
        }

        return roots;
    }
}