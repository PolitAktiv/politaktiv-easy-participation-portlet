package org.politaktiv.easyParticipation.application;

import java.util.HashSet;
import java.util.Set;

import org.politaktiv.community.application.CommunityActionConstants;

import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.model.User;
import com.liferay.portal.theme.ThemeDisplay;

public class EasyParticipationPortletUtil {
    
protected Set<Long> initializeUserGroupSet(User user) {
        
        long[] userGroupIdArray = new long[0];
        try {
            userGroupIdArray = user.getGroupIds();
        }  catch (SystemException e) {
            e.printStackTrace();
        }
        Set<Long> userGroupIds = new HashSet<Long>();
        for (int i = 0; i < userGroupIdArray.length; i++) {
            userGroupIds.add(userGroupIdArray[i]);
        }
        
        return userGroupIds;
        }
    
    protected Set<Long> initializeUserGroupSet(User user, CommunityActionConstants actionConstant, long actionCommunityId) {
        
        //Create userGroupId set before action
        long[] userGroupIdArray = new long[0];
        try {
            userGroupIdArray = user.getGroupIds();
        }  catch (SystemException e) {
            e.printStackTrace();
        }
        Set<Long> userGroupIds = new HashSet<Long>();
        for (int i = 0; i < userGroupIdArray.length; i++) {
            userGroupIds.add(userGroupIdArray[i]);
        }
        
        //process action on the userGroupId set
        //the set of groups, the user participates in, only changes instantly, when the user clicks on "join" or "leave"
        if(actionConstant.equals(CommunityActionConstants.JOIN)){
            userGroupIds.add(actionCommunityId);
        }else if(actionConstant.equals(CommunityActionConstants.LEAVE)){
            userGroupIds.remove(actionCommunityId);
        }
        
        return userGroupIds;
        }
}
