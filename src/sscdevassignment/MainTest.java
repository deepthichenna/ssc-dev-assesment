package sscdevassignment;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import sscdevassignment.Main.Task;
import sscdevassignment.Main.TaskGroup;
import sscdevassignment.Main.TaskType;

public class MainTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
    	TaskExecutorServiceImpl executorService = new TaskExecutorServiceImpl(3);

        UUID group1 = UUID.randomUUID();
        UUID group2 = UUID.randomUUID();

        TaskGroup taskGroup1 = new TaskGroup(group1);
        TaskGroup taskGroup2 = new TaskGroup(group2);

        Task<Integer> task1 = new Task<>(
            UUID.randomUUID(),
            taskGroup1,
            TaskType.READ,
            () -> {
                Thread.sleep(1000);
                System.out.println("Task 1 completed");
                return 1;
            }
        );

        Task<Integer> task2 = new Task<>(
            UUID.randomUUID(),
            taskGroup1,
            TaskType.WRITE,
            () -> {
                Thread.sleep(500);
                System.out.println("Task 2 completed");
                return 2;
            }
        );

        Task<Integer> task3 = new Task<>(
            UUID.randomUUID(),
            taskGroup2,
            TaskType.READ,
            () -> {
                Thread.sleep(200);
                System.out.println("Task 3 completed");
                return 3;
            }
        );

        Future<Integer> firstResult = executorService.submitTask(task1);
        Future<Integer> secondResult = executorService.submitTask(task2);
        Future<Integer> thirdResult = executorService.submitTask(task3);

        System.out.println("First Result " + firstResult.get());
        System.out.println("Second Result " + secondResult.get());
        System.out.println("Third Result " + thirdResult.get());

        executorService.shutdown();
    }
}
