package edu.utexas.ece.mpc.context.group;

import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.GroupContextSummary;

public interface GroupDefinition {
    public void handleContextSummary(GroupContextSummary currentGroupSummary,
                                                       ContextSummary newSummary);

    public void handleGroupSummary(GroupContextSummary currentGroupSummary,
                                   ContextSummary newGroupSummary);

    public int getId();
}
