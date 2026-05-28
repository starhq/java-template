package com.github.starhq.template.common.util;

import com.github.starhq.template.model.vo.tree.Tree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TreeBuildUtils 树形结构构建工具测试")
class TreeBuildUtilsTest {

    // ========================================
    // 测试用的内部类，
    // 模拟 Tree
    // 接口的实现
    // ========================================

    static class TestTreeNode implements Tree<TestTreeNode> {
        private Long id;
        private Long parentId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private String name;
        private List<TestTreeNode> children;

        public TestTreeNode() {
            // 模拟从数据库查出来的初始状态，children 可能是 null
            this.children = null;
        }

        public TestTreeNode(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
            this.children = null;
        }

        @Override
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        @Override
        public List<TestTreeNode> getChildren() {
            return children;
        }

        @Override
        public void setChildren(List<TestTreeNode> children) {
            this.children = children;
        }

        // 辅助方法：快速创建带默认 children 的节点
        public static TestTreeNode of(Long id, Long parentId, String name) {
            return new TestTreeNode(id, parentId, name);
        }
    }

    // ========================================
    // 1.空值与基础边界测试
    // ========================================

    @Nested
    @DisplayName("1. 空值与基础边界测试")
    class NullAndEmptyTests {

        @Test
        @DisplayName("传入 null 列表 - 应返回空集合")
        void build_nullList_shouldReturnEmptyList() {
            List<TestTreeNode> result = TreeBuildUtils.build(null);
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("传入空集合 - 应返回空集合")
        void build_emptyList_shouldReturnEmptyList() {
            List<TestTreeNode> emptyNodes = Collections.emptyList();
            List<TestTreeNode> result = TreeBuildUtils.build(emptyNodes);
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("自定义根节点传 null - 应按默认规则（parentId为null或0）处理")
        void build_customRootIdNull_shouldUseDefaultRule() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(1L, null, "根节点"),
                    TestTreeNode.of(2L, 1L, "子节点")
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list, null);
            assertThat(result).hasSize(1).extracting(TestTreeNode::getId).containsExactly(1L);
        }
    }

    // ========================================
    // 2.
    // 默认根节点识别测试(parentId ==null||0)
    // ========================================

    @Nested
    @DisplayName("2. 默认根节点识别测试")
    class DefaultRootTests {

        @Test
        @DisplayName("parentId 为 null - 应被识别为根节点")
        void build_parentIdIsNull_shouldBeRoot() {
            List<TestTreeNode> list = Collections.singletonList(TestTreeNode.of(1L, null, "根"));
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("parentId 为 0L - 应被识别为根节点")
        void build_parentIdIsZero_shouldBeRoot() {
            List<TestTreeNode> list = Collections.singletonList(TestTreeNode.of(1L, 0L, "根"));
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("存在多个根节点（混合 null 和 0L） - 应全部找出")
        void build_multipleRoots_shouldFindAll() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(1L, null, "根1(null)"),
                    TestTreeNode.of(2L, 0L, "根2(0L)"),
                    TestTreeNode.of(3L, 1L, "子节点")
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            assertThat(result).hasSize(2)
                    .extracting(TestTreeNode::getId)
                    .containsExactlyInAnyOrder(1L, 2L);
        }
    }

    // ========================================
    // 3.自定义根节点识别测试
    // ========================================

    @Nested
    @DisplayName("3. 自定义根节点识别测试")
    class CustomRootTests {

        @Test
        @DisplayName("指定 rootId 为 -1L - 只有 parentId 为 -1L 的是根节点")
        void build_customRootId_shouldMatchExactly() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(1L, -1L, "虚拟根"),
                    TestTreeNode.of(2L, null, "默认根(应被忽略)"),
                    TestTreeNode.of(3L, 1L, "子节点")
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list, -1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getId()).isEqualTo(1L);
            assertThat(result.getFirst().getChildren()).hasSize(1);
        }

        @Test
        @DisplayName("指定的 rootId 在列表中不存在 - 应返回空集合")
        void build_customRootIdNotFound_shouldReturnEmpty() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(1L, null, "节点1"),
                    TestTreeNode.of(2L, 0L, "节点2")
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list, 999L);

            assertThat(result).isEmpty();
        }
    }

    // ========================================
    // 4.树形层级结构组装测试
    // ========================================

    @Nested
    @DisplayName("4. 树形层级结构组装测试")
    class HierarchyAssemblyTests {

        @Test
        @DisplayName("两层结构（父-子） - 应正确组装")
        void build_twoLevels_shouldAssembleCorrectly() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(2L, 1L, "子节点"),
                    TestTreeNode.of(1L, 0L, "父节点") // 故意打乱顺序
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            assertThat(result).hasSize(1);
            TestTreeNode root = result.getFirst();
            assertThat(root.getId()).isEqualTo(1L);
            assertThat(root.getChildren()).hasSize(1);
            assertThat(root.getChildren().getFirst().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("三层结构（祖-父-子） - 应正确递归组装")
        void build_threeLevels_shouldAssembleRecursively() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(3L, 2L, "孙节点"),
                    TestTreeNode.of(1L, null, "祖节点"),
                    TestTreeNode.of(2L, 1L, "父节点")
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            // 验证第一层
            TestTreeNode root = result.getFirst();
            assertThat(root.getId()).isEqualTo(1L);

            // 验证第二层
            assertThat(root.getChildren()).hasSize(1);
            TestTreeNode child = root.getChildren().getFirst();
            assertThat(child.getId()).isEqualTo(2L);

            // 验证第三层
            assertThat(child.getChildren()).hasSize(1);
            TestTreeNode grandChild = child.getChildren().getFirst();
            assertThat(grandChild.getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("一个父节点有多个子节点 - 应全部归并到同一个 children 下")
        void build_multipleChildren_shouldMergeUnderSameParent() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(1L, 0L, "父节点"),
                    TestTreeNode.of(2L, 1L, "子节点A"),
                    TestTreeNode.of(3L, 1L, "子节点B"),
                    TestTreeNode.of(4L, 1L, "子节点C")
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getChildren())
                    .hasSize(3)
                    .extracting(TestTreeNode::getId)
                    .containsExactlyInAnyOrder(2L, 3L, 4L);
        }

        @Test
        @DisplayName("✅ 验证源码关键逻辑：所有节点的 children 都被强制初始化（叶子节点不应为 null）")
        void build_leafNodes_shouldHaveEmptyChildrenList() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(1L, 0L, "父节点"),
                    TestTreeNode.of(2L, 1L, "叶子节点")
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            TestTreeNode leaf = result.getFirst().getChildren().getFirst();
            // 如果没有第一层循环的初始化，这里可能是 null
            assertThat(leaf.getChildren()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("✅ 验证源码关键逻辑：即使原对象自带的 children 不为空，也会被覆盖重置")
        void build_existingChildren_shouldBeOverridden() {
            TestTreeNode parent = TestTreeNode.of(1L, 0L, "父节点");
            TestTreeNode child = TestTreeNode.of(2L, 1L, "子节点");

            // 模拟脏数据：子节点自己带了伪造的 children
            child.setChildren(new ArrayList<>());
            child.getChildren().add(TestTreeNode.of(99L, 2L, "脏数据孙子"));

            List<TestTreeNode> result = TreeBuildUtils.build(Arrays.asList(parent, child));

            // 因为 build 方法第一层循环强制 new ArrayList<>()，所以原有的脏数据孙子应该被清空
            TestTreeNode actualChild = result.getFirst().getChildren().getFirst();
            assertThat(actualChild.getChildren()).isEmpty();
        }
    }

    // ========================================
    // 5.脏数据与异常容错测试
    // ========================================

    @Nested
    @DisplayName("5. 脏数据与异常容错测试")
    class DirtyDataTests {

        @Test
        @DisplayName("孤儿节点（parentId 找不到对应的父节点） - 应被静默丢弃")
        void build_orphanNode_shouldBeIgnored() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(1L, 0L, "合法根节点"),
                    TestTreeNode.of(2L, 999L, "孤儿节点") // 父节点 999 不存在
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getChildren()).isEmpty(); // 孤儿没加进来
        }

        @Test
        @DisplayName("列表中存在重复的 ID - 后出现的节点会覆盖先出现的节点（Map特性）")
        void build_duplicateIds_lastOneWinsInMap() {
            List<TestTreeNode> list = Arrays.asList(
                    TestTreeNode.of(1L, 0L, "根节点V1"),
                    TestTreeNode.of(2L, 1L, "属于V1的子节点"),
                    TestTreeNode.of(1L, 0L, "根节点V2(同ID覆盖)") // 同样的ID
            );
            List<TestTreeNode> result = TreeBuildUtils.build(list);

            // 因为 HashMap put 会让 V2 覆盖 V1，而在第二次循环时，V2 被当成 root 加入，
            // V1 已经从 map 中丢失了，"属于V1的子节点" 找到的 parent 实际上是 V2。
            assertThat(result).hasSize(2);
            assertThat(result.getFirst().getName()).isEqualTo("根节点V1");

            // 子节点会挂到覆盖后的 V2 下面（因为Map里1L对应的已经是V2了）
            assertThat(result.getFirst().getChildren()).hasSize(0);
        }

        @Test
        @DisplayName("自引用节点（自己的 parentId 指向自己） - 应被识别为根节点，且无子节点")
        void build_selfReferencingNode_shouldBeRootWithNoChildren() {
            List<TestTreeNode> list = Collections.singletonList(
                    TestTreeNode.of(1L, 1L, "我是我自己的爸爸")
            );
            // 默认规则下，parentId=1 不等于 null 且不等于 0，所以它不是根节点
            List<TestTreeNode> defaultResult = TreeBuildUtils.build(list);
            assertThat(defaultResult).isEmpty(); // 被当成孤儿节点丢弃

            // 如果自定义 rootId = 1L
            List<TestTreeNode> customResult = TreeBuildUtils.build(list, 1L);
            assertThat(customResult).hasSize(1);
            assertThat(customResult.getFirst().getChildren()).isEmpty(); // 防止死循环，它只会把自己加到 roots，不会往自己 children 里加
        }
    }
}
