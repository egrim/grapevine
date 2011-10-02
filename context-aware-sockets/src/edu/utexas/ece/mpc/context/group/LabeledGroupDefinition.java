package edu.utexas.ece.mpc.context.group;

import java.util.Set;

import edu.utexas.ece.mpc.context.ContextHandler;
import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.GroupContextSummary;
import edu.utexas.ece.mpc.context.util.GroupUtils;

public class LabeledGroupDefinition implements GroupDefinition {
    private static final ContextHandler handler = ContextHandler.getInstance();
    private final int gId;

    public LabeledGroupDefinition(int gId) {
        this.gId = gId;
    }

    @Override
    public int getId() {
        return gId;
    }

    @Override
    public void handleContextSummary(GroupContextSummary currentGroupSummary,
                                     ContextSummary newSummary) {
        int id = newSummary.getId();
        int gId = currentGroupSummary.getId();
        Set<Integer> groupIds = GroupUtils.getDeclaredMemberships(newSummary);
        if (groupIds.contains(gId)) {
            if (!currentGroupSummary.getMemberIds().contains(id)) {
                handler.logDbg("Adding member " + id + " to group " + gId);
                currentGroupSummary.addMemberId(id);
            }
        }
    }

    @Override
    public void handleGroupSummary(GroupContextSummary currentGroupSummary,
                                   ContextSummary newGroupSummary) {
        Set<Integer> memberIds = currentGroupSummary.getMemberIds();
        Set<Integer> newMemberIds = GroupUtils.getGroupMembers(newGroupSummary);
        newMemberIds.removeAll(memberIds);
        if (!newMemberIds.isEmpty()) {
            handler.logDbg("Adding members " + newMemberIds + " to group " + gId);
            currentGroupSummary.addMemberIds(newMemberIds);
        }
    }

}
