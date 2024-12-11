package sscdevassignment;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import sscdevassignment.Main.Task;
import sscdevassignment.Main.TaskExecutor;

public class TaskExecutorServiceImpl implements TaskExecutor {

	private final ExecutorService executor;
	private final Map<UUID, BlockingQueue<Runnable>> groupQueueMap = new ConcurrentHashMap<>();
	private final ReentrantLock groupLock = new ReentrantLock();

	public TaskExecutorServiceImpl(int maxConcurrency) {
		this.executor = Executors.newFixedThreadPool(maxConcurrency);
	}

	@Override
	public <T> Future<T> submitTask(Task<T> task) {
		Objects.requireNonNull(task, "Task must not be null");

		CompletableFuture<T> future = new CompletableFuture<>();
		UUID groupId = task.taskGroup().groupUUID();

		Runnable wrappedTask = () -> {
			try {
				T result = task.taskAction().call();
				future.complete(result);
			} catch (Exception e) {
				future.completeExceptionally(e);
			} finally {
				completeTask(groupId);
			}
		};
		groupQueueMap.computeIfAbsent(groupId, id -> new LinkedBlockingQueue<>()).offer(wrappedTask);
		startNextTask(groupId);
		return future;
	}

	private void startNextTask(UUID groupId) {
		groupLock.lock();
		try {
			BlockingQueue<Runnable> queue = groupQueueMap.get(groupId);
			if (queue != null && !queue.isEmpty()) {
				executor.submit(queue.poll());
			}
		} finally {
			groupLock.unlock();
		}
	}

	private void completeTask(UUID groupId) {
		startNextTask(groupId);
		groupLock.lock();
		try {
			if (groupQueueMap.get(groupId).isEmpty()) {
				groupQueueMap.remove(groupId);
			}
		} finally {
			groupLock.unlock();
		}
	}

	public void shutdown() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
		}
	}
}