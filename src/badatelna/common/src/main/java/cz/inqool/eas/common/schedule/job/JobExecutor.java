package cz.inqool.eas.common.schedule.job;

import cz.inqool.eas.common.schedule.run.Run;
import cz.inqool.eas.common.schedule.run.RunRepository;
import cz.inqool.eas.common.schedule.run.RunState;
import cz.inqool.eas.common.script.ScriptExecutor;
import cz.inqool.eas.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.time.Instant;

@Slf4j
public class JobExecutor {
    private ScriptExecutor scriptExecutor;

    private RunRepository runRepository;

    private TransactionTemplate transactionTemplate;

    public void executeJob(Job job) {
        Run run = startExecution(job);

        ByteArrayOutputStream console = new ByteArrayOutputStream();

        RunState state = RunState.FINISHED;
        Object result = null;
        try {
            result = scriptExecutor.executeScript(
                    job.getScriptType(),
                    job.getScript(),
                    job.isUseTransaction(),
                    null,
                    console
            );
        } catch (Exception e) {
            state = RunState.ERROR;
            result = e.getMessage();
        } finally {
            endExecution(run, state, console.toString(Charset.defaultCharset()), result);
        }
    }

    protected Run startExecution(Job job) {
        Run run = new Run();
        run.setJob(job);
        run.setState(RunState.STARTED);
        run.setStartTime(Instant.now());

        return transactionTemplate.execute((status) -> runRepository.create(run));
    }

    protected void endExecution(Run run, RunState state, String console, Object result) {
        run.setState(state);
        run.setConsole(console);
        run.setEndTime(Instant.now());
        if (result != null) {
            run.setResult(JsonUtils.toJsonString(result));
        }

        transactionTemplate.executeWithoutResult((status) -> runRepository.update(run));
    }

    @Autowired
    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Autowired
    public void setScriptExecutor(ScriptExecutor scriptExecutor) {
        this.scriptExecutor = scriptExecutor;
    }

    @Autowired
    public void setRunRepository(RunRepository runRepository) {
        this.runRepository = runRepository;
    }
}
