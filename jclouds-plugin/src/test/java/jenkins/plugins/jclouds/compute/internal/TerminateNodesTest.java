package jenkins.plugins.jclouds.compute.internal;

import java.util.List;

import org.junit.Test;
import org.junit.Rule;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;


import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.logging.Logger;

import shaded.com.google.common.base.Function;
import shaded.com.google.common.base.Functions;
import shaded.com.google.common.cache.CacheBuilder;
import shaded.com.google.common.cache.CacheLoader;
import shaded.com.google.common.cache.LoadingCache;
import shaded.com.google.common.collect.ImmutableList;
import shaded.com.google.common.collect.ImmutableMap;
import shaded.com.google.common.collect.Iterables;
import shaded.com.google.common.collect.Lists;

import org.jvnet.hudson.test.JenkinsRule;

public class TerminateNodesTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    private static ComputeService compute;

    @BeforeClass
    public static void setUp() throws Exception {
        compute = ContextBuilder.newBuilder("stub").buildView(ComputeServiceContext.class).getComputeService();
    }

    @Test
    public void testSuspendOnlySuspendsNodesInQuestion() throws InterruptedException, ExecutionException, RunNodesException {

        List<NodeMetadata> nodes = ImmutableList.copyOf(compute.createNodesInGroup("suspend", 10));
        List<List<NodeMetadata>> split = Lists.partition(nodes, 5);

        Iterable<RunningNode> runningNodesToSuspend = Iterables.transform(split.get(0), new Function<NodeMetadata, RunningNode>() {

            public RunningNode apply(NodeMetadata input) {
                return new RunningNode("stub", "template", true, input);
            }

        });

        newTerminateNodes(compute).apply(runningNodesToSuspend);

        for (NodeMetadata node : split.get(0))
            assertEquals(NodeMetadata.Status.SUSPENDED, compute.getNodeMetadata(node.getId()).getStatus());
        for (NodeMetadata node : split.get(1))
            assertEquals(NodeMetadata.Status.RUNNING, compute.getNodeMetadata(node.getId()).getStatus());

    }

    private TerminateNodes newTerminateNodes(ComputeService compute) {
        LoadingCache<String, ComputeService> cache = CacheBuilder.newBuilder().build(
                CacheLoader.<String, ComputeService>from(Functions.forMap(ImmutableMap.of("stub", compute))));
        return new TerminateNodes(Logger.NULL, cache);
    }

    @Test
    public void testDestroyOnlyDestroysNodesInQuestion() throws InterruptedException, ExecutionException, RunNodesException {

        List<NodeMetadata> nodes = ImmutableList.copyOf(compute.createNodesInGroup("destroy", 10));
        List<List<NodeMetadata>> split = Lists.partition(nodes, 5);

        Iterable<RunningNode> runningNodesToDestroy = Iterables.transform(split.get(0), new Function<NodeMetadata, RunningNode>() {

            public RunningNode apply(NodeMetadata input) {
                return new RunningNode("stub", "template", false, input);
            }

        });

        newTerminateNodes(compute).apply(runningNodesToDestroy);

        for (NodeMetadata node : split.get(0))
            assertEquals(null, compute.getNodeMetadata(node.getId()));
        for (NodeMetadata node : split.get(1))
            assertEquals(NodeMetadata.Status.RUNNING, compute.getNodeMetadata(node.getId()).getStatus());

    }

    @Test
    public void testSuspendAndDestroy() throws InterruptedException, ExecutionException, RunNodesException {

        List<NodeMetadata> nodes = ImmutableList.copyOf(compute.createNodesInGroup("suspenddestroy", 10));
        List<List<NodeMetadata>> split = Lists.partition(nodes, 5);

        Iterable<RunningNode> runningNodesToSuspend = Iterables.transform(split.get(0), new Function<NodeMetadata, RunningNode>() {

            public RunningNode apply(NodeMetadata input) {
                return new RunningNode("stub", "template", true, input);
            }

        });

        Iterable<RunningNode> runningNodesToDestroy = Iterables.transform(split.get(1), new Function<NodeMetadata, RunningNode>() {

            public RunningNode apply(NodeMetadata input) {
                return new RunningNode("stub", "template", false, input);
            }

        });

        newTerminateNodes(compute).apply(Iterables.concat(runningNodesToSuspend, runningNodesToDestroy));

        for (NodeMetadata node : split.get(0))
            assertEquals(NodeMetadata.Status.SUSPENDED, compute.getNodeMetadata(node.getId()).getStatus());
        for (NodeMetadata node : split.get(1))
            assertEquals(null, compute.getNodeMetadata(node.getId()));

    }

    @AfterClass
    public static void tearDown() throws Exception {
        compute.getContext().close();
    }
}
