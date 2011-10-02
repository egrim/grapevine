package edu.utexas.ece.mpc.context.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.utexas.ece.mpc.context.summary.ContextSummary;
import edu.utexas.ece.mpc.context.summary.HashMapContextSummary;
import edu.utexas.ece.mpc.context.summary.LabeledContextSummary;
import edu.utexas.ece.mpc.context.summary.WireContextSummary;

public class GroupUtils {
    public static final int GROUP_ID_OFFSET = 10000;
    public static final String MEMBER_PREFIX = "Member";
    public static final String MEMBERS_ENUMERATED = "MembersEnumerated";
    public static final String GROUP_DECLARATION_PREFIX = "Group";
    public static final String ID_AGGREGATION_PREFIX = "Id";
    public static final String GROUP_DECLARATIONS_ENUMERATED = "GroupsEnumerated";
    public static final String IDS_AGGREGATED = "IdsAggregated";

    public static boolean declaresGroupMembership(ContextSummary summary, int gIdToCheck) {
        Set<Integer> groupMemberships = getDeclaredMemberships(summary);
        return groupMemberships.contains(gIdToCheck);
    }

    public static void addDeclaredGroupMembership(HashMapContextSummary summary, int gId) {
        Set<Integer> declaredMemberships = getDeclaredMemberships(summary);
        declaredMemberships.add(gId);
        
        int groupsEnumerated = 0;
        for (Integer declaredMembership: declaredMemberships) {
            summary.put(GROUP_DECLARATION_PREFIX + groupsEnumerated, declaredMembership);
            groupsEnumerated++;
        }
        summary.put(GROUP_DECLARATIONS_ENUMERATED, groupsEnumerated);
    }

    public static Set<Integer> getDeclaredMemberships(ContextSummary summary) {
        Set<Integer> ids = new HashSet<Integer>();
        Integer groupsEnumerated = summary.get(GROUP_DECLARATIONS_ENUMERATED);
        if (groupsEnumerated != null) {
            for (int i = 0; i < groupsEnumerated; i++) {
                ids.add(summary.get(GROUP_DECLARATION_PREFIX + i));
            }
        }

        return ids;
    }

    public static void addGroupMember(HashMapContextSummary groupSummary, int id) {
        Integer membersEnumerated;
        if (groupSummary.containsKey(MEMBERS_ENUMERATED)) {
            membersEnumerated = groupSummary.get(MEMBERS_ENUMERATED);
        } else {
            membersEnumerated = 0;
        }
        
        groupSummary.put(MEMBER_PREFIX + membersEnumerated, id);
        groupSummary.put(MEMBERS_ENUMERATED, membersEnumerated + 1);
    }

    public static Set<Integer> getGroupMembers(ContextSummary groupSummary) {
        Set<Integer> ids = new HashSet<Integer>();
        Integer membersEnumerated = groupSummary.get(MEMBERS_ENUMERATED);
        if (membersEnumerated != null) {
            for (int i = 0; i < membersEnumerated; i++) {
                ids.add(groupSummary.get(MEMBER_PREFIX + i));
            }
        }

        return ids;
    }

    public static void setGroupMembers(HashMapContextSummary groupSummary, Set<Integer> memberIds) {
        Integer previousNumberOfMembers = groupSummary.get(MEMBERS_ENUMERATED);
        int newNumberOfMembers = memberIds.size();

        groupSummary.put(MEMBERS_ENUMERATED, 0);
        for (Integer memberId: memberIds) {
            addGroupMember(groupSummary, memberId);
        }

        if (previousNumberOfMembers != null) {
            for (int i = newNumberOfMembers; i < previousNumberOfMembers; i++) {
                groupSummary.remove(MEMBER_PREFIX + i);

            }
        }
    }

    public static boolean isAggregated(ContextSummary summary, int idToCheck) {
        Set<Integer> idsAggregated = getAggregatedIds(summary);
        return idsAggregated.contains(idToCheck);
    }

    public static boolean haveNoCommonAggregation(ContextSummary summary1, ContextSummary summary2) {
        Set<Integer> summary1Ids = getAggregatedIds(summary1);
        Set<Integer> summary2Ids = getAggregatedIds(summary2);
        summary2Ids.retainAll(summary1Ids);

        return summary2Ids.size() == 0;
    }

    public static Set<Integer> getAggregatedIds(ContextSummary summary) {
        Set<Integer> ids = new HashSet<Integer>();
        Integer idsAggregated = summary.get(IDS_AGGREGATED);
        if (idsAggregated != null) {
            for (int i = 0; i < idsAggregated; i++) {
                ids.add(summary.get(ID_AGGREGATION_PREFIX + i));
            }
        }

        return ids;
    }

    public static void aggregateIntoGroupSummary(LabeledContextSummary groupSummary,
                                                  ContextSummary summary) {
        Set<Integer> memberIds = getGroupMembers(groupSummary);
        memberIds.addAll(getGroupMembers(summary));

        int memberIdCount = 0;
        for (Integer memberId: memberIds) {
            groupSummary.put(MEMBER_PREFIX + memberIdCount, memberId);
            memberIdCount++;
        }
        groupSummary.put(MEMBERS_ENUMERATED, memberIdCount);

        Set<Integer> aggregatedIds = getAggregatedIds(groupSummary);
        aggregatedIds.addAll(getAggregatedIds(summary));

        int aggregatedIdCount = 0;
        for (Integer aggregatedId: aggregatedIds) {
            groupSummary.put(ID_AGGREGATION_PREFIX + aggregatedIdCount, aggregatedId);
            aggregatedIdCount++;
        }

        groupSummary.put(IDS_AGGREGATED, aggregatedIdCount);
    }

    // public static LabeledContextSummary createGroupAgg(int gId) {
    // ContextHandler handler = ContextHandler.getInstance();
    // LabeledContextSummary groupSummary = new LabeledContextSummary(gId);
    // updateGroupAgg(groupSummary, handler.getReceivedSummaries());
    // return groupSummary;
    // }

    public static void updateGroupAgg(LabeledContextSummary groupSummary,
                                      Collection<WireContextSummary> summaries) {
        int gId = groupSummary.getId();
        for (ContextSummary summary: summaries) {
            if ((declaresGroupMembership(summary, gId) || summary.getId() == gId) // If it's a summary
                                                                         // claiming membership or
                                                                         // is an aggregated group
                                                                         // summary for the group
                && !isAggregated(groupSummary, summary.getId())
                && haveNoCommonAggregation(groupSummary, summary)) {
                aggregateIntoGroupSummary(groupSummary, summary);
            }
        }
    }

}
