package edu.utexas.ece.mpc.context.summary;

import java.util.Collection;
import java.util.Set;

public interface GroupContextSummary extends ContextSummary {

    GroupContextSummary getGroupCopy();

    Set<Integer> getMemberIds();

    void setMemberIds(Set<Integer> memberIds);

    void addMemberId(int id);

    void addMemberIds(Collection<Integer> ids);

}
