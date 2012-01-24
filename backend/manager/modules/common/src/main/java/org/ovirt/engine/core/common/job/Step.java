package org.ovirt.engine.core.common.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.IVdcQueryable;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.NGuid;

/**
 * represents a meaningful phase of the Job. A Step could be a parent of other steps (e.g. step named EXECUTION could
 * have a list of steps beneath it which are also part of the job)
 */
public class Step extends IVdcQueryable implements BusinessEntity<Guid> {

    /**
     * Automatic generated serial version ID
     */
    private static final long serialVersionUID = 3711656756401350600L;

    /**
     * The Step ID uniquely identifies a disk in the system.
     */
    private Guid id;

    /**
     * The job which the step comprises
     */
    private Guid jobId;

    /**
     * The direct parent step of the current step
     */
    private NGuid parentStepId;

    /**
     * The step type
     */
    private StepEnum stepType;

    /**
     * The description of the step
     */
    private String description;

    /**
     * The order of the step in current hierarchy level
     */
    private int stepNumber;

    /**
     * The status of the step
     */
    private JobExecutionStatus status = JobExecutionStatus.STARTED;

    /**
     * The start time of the step
     */
    private Date startTime;

    /**
     * The end time of the step
     */
    private Date endTime;

    /**
     * A pass-thru string to identify this step as part of a wider action
     */
    private String correlationId;

    /**
     * An external system referenced by the step (e.g. VDSM)
     */
    private ExternalSystem externalSystem = new ExternalSystem();

    /**
     * The successors steps
     */
    private List<Step> steps = new ArrayList<Step>();

    public Step() {
    }

    public Step(StepEnum stepType) {
        this.id = Guid.NewGuid();
        this.parentStepId = null;
        this.stepType = stepType;
        this.startTime = new Date();
    }

    public Step(StepEnum stepType, String description) {
        this(stepType);
        if (description != null) {
            setDescription(description);
        } else {
            setDescription(getStepName());
        }
    }

    public StepEnum getStepType() {
        return stepType;
    }

    public void setStepType(StepEnum stepType) {
        this.stepType = stepType;
    }

    public String getStepName() {
        return stepType.name();
    }

    public void setJobId(Guid jobId) {
        this.jobId = jobId;
    }

    public Guid getJobId() {
        return jobId;
    }

    public void setParentStepId(NGuid parentStepId) {
        this.parentStepId = parentStepId;
    }

    public NGuid getParentStepId() {
        return parentStepId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setExternalSystem(ExternalSystem externalSystem) {
        this.externalSystem = externalSystem;
    }

    public ExternalSystem getExternalSystem() {
        return externalSystem;
    }

    public void setStepNumber(int position) {
        this.stepNumber = position;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    @Override
    public ArrayList<String> getChangeablePropertiesList() {
        return new ArrayList<String>();
    }

    @Override
    public Object getQueryableId() {
        return id;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Step addStep(StepEnum childStepType, String description) {
        Step childStep = new Step(childStepType);
        childStep.setParentStepId(id);
        childStep.setJobId(jobId);
        childStep.setStepNumber(getSteps().size());
        childStep.setCorrelationId(correlationId);

        if (description == null) {
            childStep.setDescription(childStep.getStepName());
        } else {
            childStep.setDescription(description);
        }
        steps.add(childStep);
        return childStep;
    }

    public void markStepEnded(boolean isSuccess) {
        endTime = new Date();
        if (isSuccess) {
            setStatus(JobExecutionStatus.FINISHED);
        } else {
            setStatus(JobExecutionStatus.FAILED);
        }
    }

    /**
     * Set completion status to the step which its current status isn't completed yet.
     * The step status won't be changed if the status to update matches the current status.
     *
     * @param exitStatus
     *            The completion status of the step
     */
    public void markStepEnded(JobExecutionStatus exitStatus) {
        if (status == JobExecutionStatus.STARTED || status == JobExecutionStatus.UNKNOWN) {
            if (exitStatus != JobExecutionStatus.STARTED && status != exitStatus) {
                status = exitStatus;
                endTime = new Date();
            }
        }
    }

    @Override
    public Guid getId() {
        return id;
    }

    @Override
    public void setId(Guid id) {
        this.id = id;
    }

    public void setStatus(JobExecutionStatus status) {
        this.status = status;
    }

    public JobExecutionStatus getStatus() {
        return status;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public List<Step> getSteps() {
        return steps;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((correlationId == null) ? 0 : correlationId.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
        result = prime * result + ((externalSystem == null) ? 0 : externalSystem.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((jobId == null) ? 0 : jobId.hashCode());
        result = prime * result + ((parentStepId == null) ? 0 : parentStepId.hashCode());
        result = prime * result + ((startTime == null) ? 0 : startTime.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + stepNumber;
        result = prime * result + ((stepType == null) ? 0 : stepType.hashCode());
        result = prime * result + ((steps == null) ? 0 : steps.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Step)) {
            return false;
        }
        Step other = (Step) obj;
        if (correlationId == null) {
            if (other.correlationId != null) {
                return false;
            }
        } else if (!correlationId.equals(other.correlationId)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (endTime == null) {
            if (other.endTime != null) {
                return false;
            }
        } else if (!endTime.equals(other.endTime)) {
            return false;
        }
        if (externalSystem == null) {
            if (other.externalSystem != null) {
                return false;
            }
        } else if (!externalSystem.equals(other.externalSystem)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (jobId == null) {
            if (other.jobId != null) {
                return false;
            }
        } else if (!jobId.equals(other.jobId)) {
            return false;
        }
        if (parentStepId == null) {
            if (other.parentStepId != null) {
                return false;
            }
        } else if (!parentStepId.equals(other.parentStepId)) {
            return false;
        }
        if (startTime == null) {
            if (other.startTime != null) {
                return false;
            }
        } else if (!startTime.equals(other.startTime)) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        if (stepNumber != other.stepNumber) {
            return false;
        }
        if (stepType != other.stepType) {
            return false;
        }
        if (steps == null) {
            if (other.steps != null) {
                return false;
            }
        } else if (!steps.equals(other.steps)) {
            return false;
        }
        return true;
    }

}
