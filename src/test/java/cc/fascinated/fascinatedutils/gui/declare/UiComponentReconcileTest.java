package cc.fascinated.fascinatedutils.gui.declare;

import cc.fascinated.fascinatedutils.gui.widgets.FReconcileRoot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UiComponentReconcileTest {

    @Test
    void mountInstanceIsReusedAcrossReconcilesWhenTypeMatches() {
        UiReconciler reconciler = new UiReconciler();
        FReconcileRoot root = new FReconcileRoot();
        UiReconciler.MountNode rootMount = new UiReconciler.MountNode();
        MountTracker.resetCounters();

        reconciler.sync(root, rootMount, Ui.component(TrivialComponent.class, TrivialComponent::new, "first"));
        TrivialComponent firstInstance = MountTracker.lastInstance;
        int mountCountAfterFirst = MountTracker.mountCount;

        reconciler.sync(root, rootMount, Ui.component(TrivialComponent.class, TrivialComponent::new, "second"));

        Assertions.assertSame(firstInstance, MountTracker.lastInstance, "component instance should survive reconcile");
        Assertions.assertEquals(mountCountAfterFirst, MountTracker.mountCount, "onMount must not fire twice for same instance");
        Assertions.assertEquals("second", firstInstance.lastRenderedProp, "bindProps must push latest props before render");
    }

    @Test
    void componentUnmountsWhenSubtreeIsReplaced() {
        UiReconciler reconciler = new UiReconciler();
        FReconcileRoot root = new FReconcileRoot();
        UiReconciler.MountNode rootMount = new UiReconciler.MountNode();
        MountTracker.resetCounters();

        reconciler.sync(root, rootMount, Ui.component(TrivialComponent.class, TrivialComponent::new, "alive"));
        TrivialComponent live = MountTracker.lastInstance;

        reconciler.sync(root, rootMount, Ui.spacer(1f, 1f));

        Assertions.assertEquals(1, MountTracker.unmountCount, "onUnmount must fire exactly once when component is removed");
        Assertions.assertFalse(live.isMountedForTest(), "component mounted flag should be cleared");
    }

    @Test
    void componentUnmountsWhenTypeChanges() {
        UiReconciler reconciler = new UiReconciler();
        FReconcileRoot root = new FReconcileRoot();
        UiReconciler.MountNode rootMount = new UiReconciler.MountNode();
        MountTracker.resetCounters();

        reconciler.sync(root, rootMount, Ui.component(TrivialComponent.class, TrivialComponent::new, "first"));
        TrivialComponent firstInstance = MountTracker.lastInstance;

        reconciler.sync(root, rootMount, Ui.component(OtherComponent.class, OtherComponent::new, 0));

        Assertions.assertFalse(firstInstance.isMountedForTest(), "prior component must receive onUnmount on type swap");
        Assertions.assertEquals(1, MountTracker.unmountCount);
    }

    @Test
    void disposeAllFiresOnUnmountForEveryComponentInSubtree() {
        UiReconciler reconciler = new UiReconciler();
        FReconcileRoot root = new FReconcileRoot();
        UiReconciler.MountNode rootMount = new UiReconciler.MountNode();
        MountTracker.resetCounters();

        reconciler.sync(root, rootMount, Ui.component(NestingComponent.class, NestingComponent::new, "nested"));
        int mountedBeforeDispose = MountTracker.mountCount;

        reconciler.disposeAll(rootMount);

        Assertions.assertEquals(mountedBeforeDispose, MountTracker.unmountCount,
                "every mounted component in the subtree must receive onUnmount during dispose");
    }

    private static final class MountTracker {
        static int mountCount;
        static int unmountCount;
        static TrivialComponent lastInstance;

        static void resetCounters() {
            mountCount = 0;
            unmountCount = 0;
            lastInstance = null;
        }
    }

    public static final class TrivialComponent extends UiComponent<String> {
        private String lastRenderedProp;

        @Override
        protected void onMount() {
            MountTracker.mountCount++;
            MountTracker.lastInstance = this;
        }

        @Override
        protected void onUnmount() {
            MountTracker.unmountCount++;
        }

        @Override
        public UiView render() {
            lastRenderedProp = props();
            return Ui.spacer(0f, 0f);
        }

        boolean isMountedForTest() {
            return isMounted();
        }
    }

    public static final class OtherComponent extends UiComponent<Integer> {
        @Override
        protected void onMount() {
            MountTracker.mountCount++;
        }

        @Override
        protected void onUnmount() {
            MountTracker.unmountCount++;
        }

        @Override
        public UiView render() {
            return Ui.spacer(0f, 0f);
        }
    }

    public static final class NestingComponent extends UiComponent<String> {
        @Override
        protected void onMount() {
            MountTracker.mountCount++;
        }

        @Override
        protected void onUnmount() {
            MountTracker.unmountCount++;
        }

        @Override
        public UiView render() {
            return Ui.component(TrivialComponent.class, TrivialComponent::new, props());
        }
    }
}
