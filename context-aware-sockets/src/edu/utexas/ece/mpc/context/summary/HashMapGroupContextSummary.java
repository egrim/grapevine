package edu.utexas.ece.mpc.context.summary;

import java.util.Collection;
import java.util.Set;

import edu.utexas.ece.mpc.context.util.GroupUtils;

@SuppressWarnings("serial")
public class HashMapGroupContextSummary extends HashMapContextSummary implements GroupContextSummary {

    public HashMapGroupContextSummary(int gId) {
        super(gId);
    }

    public HashMapGroupContextSummary(HashMapGroupContextSummary other) {
        super(other);
    }

    @Override
    public GroupContextSummary getGroupCopy() {
        return new HashMapGroupContextSummary(this);
    }

    @Override
    public Set<Integer> getMemberIds() {
        return GroupUtils.getGroupMembers(this);
    }

    @Override
    public void setMemberIds(Set<Integer> memberIds) {
        GroupUtils.setGroupMembers(this, memberIds);
    }

    @Override
    public void addMemberId(int id) {
        GroupUtils.addGroupMember(this, id);
    }

    @Override
    public void addMemberIds(Collection<Integer> ids) {
        for (Integer id: ids) {
            addMemberId(id);
        }
    }

}
