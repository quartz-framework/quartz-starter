package xyz.quartzframework.bungee.scheduler;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import xyz.quartzframework.bungee.session.BungeeSession;
import xyz.quartzframework.core.bean.annotation.Inject;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanPresent;
import xyz.quartzframework.core.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;

@Injectable
@ActivateWhenBeanPresent(TaskScheduler.class)
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class BungeeScheduler implements Scheduler {

    private final BungeeSession session;

    private final Plugin plugin;

    private final TaskScheduler scheduler;

    @Override
    public int scheduleSyncDelayedTask(Runnable task, long delay) {
        ScheduledTask scheduledTask = scheduler.schedule(plugin, session.wrap(task), delay, TimeUnit.MILLISECONDS);
        return scheduledTask.getId();
    }

    @Override
    public int scheduleSyncDelayedTask(Runnable task) {
        return scheduleSyncDelayedTask(task, 0);
    }

    @Override
    public int scheduleSyncRepeatingTask(Runnable task, long delay, long period) {
        ScheduledTask scheduledTask = scheduler.schedule(plugin, session.wrap(task), delay, period, TimeUnit.MILLISECONDS);
        return scheduledTask.getId();
    }

    @Override
    public void cancelTask(int taskId) {
        scheduler.cancel(taskId);
    }

    @Override
    public boolean isCurrentlyRunning(int taskId) {
        return false;
    }

    @Override
    public boolean isQueued(int taskId) {
        return false;
    }
}