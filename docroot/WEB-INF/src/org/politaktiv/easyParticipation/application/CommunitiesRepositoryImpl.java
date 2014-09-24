/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 *        
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.politaktiv.easyParticipation.application;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.politaktiv.community.domain.CommunitiesRepository;
import org.politaktiv.community.domain.Community;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.OrderFactoryUtil;
import com.liferay.portal.kernel.dao.orm.RestrictionsFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PortalClassLoaderUtil;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.model.MembershipRequest;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.MembershipRequestLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.service.UserServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;

public class CommunitiesRepositoryImpl implements CommunitiesRepository {

    private static Log _log = LogFactoryUtil.getLog(CommunitiesRepositoryImpl.class);

    public List<Community> findCommunitiesByCompanyId(long companyId) {

        List<Group> currentPublicGroups = this.getAllGroupsFromCompanyId(companyId);
        HashMap<Long, Long> imageIdsToGroupIdsWithNameLogo = this
                .getImageFolderIdsToGroupIdsWithNameLogoFromCompany(companyId,
                        currentPublicGroups);
        HashMap<Long, Integer> numberOfMembersByGroupId = this
                .getNumberOfMembersFromCommunityById(currentPublicGroups);

        List<Community> returnCommunities = new ArrayList<Community>();
        for (Group group : currentPublicGroups) {

            if (imageIdsToGroupIdsWithNameLogo.containsKey(group.getGroupId())) {
                returnCommunities
                        .add(new CommunityFactoryImpl().createCommunity(
                                group.getName(),
                                group.getGroupId(),
                                imageIdsToGroupIdsWithNameLogo.get(group
                                        .getGroupId()),
                                numberOfMembersByGroupId.get(group.getGroupId()),
                                group.getFriendlyURL(), group.getType()));
            } else {
                returnCommunities
                        .add(new CommunityFactoryImpl().createCommunity(
                                group.getName(),
                                group.getGroupId(),
                                numberOfMembersByGroupId.get(group.getGroupId()),
                                group.getFriendlyURL(), group.getType()));
            }
        }

        return returnCommunities;

    }

    public void joinCommunity(long userId, long communityId) {

        // TODO: review mje 15.11.: Wie wollen hier mit Fehlern umgehen?

        // insert userId into array, because of service parameter
        long userIdArray[] = { userId };

        // add user to community
        try {
            UserServiceUtil.addGroupUsers(communityId, userIdArray,
                    new ServiceContext());
        } catch (PortalException e) {
            _log.error("error while trying to join communitiy: " + e);
        } catch (SystemException e) {
            _log.error("error while trying to join communitiy: " + e);
        }
    }


    public void requestMembershipToCommunity(long currentUserId,
            long currentCompanyId, long communityId, long currentGuestUserId)
            throws Exception {

        if (currentUserId == currentGuestUserId)
            throw new Exception(
                    "Current User and current Guest User have the same UserID");

        try {

            MembershipRequest membershipRequest = MembershipRequestLocalServiceUtil
                    .createMembershipRequest(CounterLocalServiceUtil
                            .increment());

            membershipRequest.setCompanyId(currentCompanyId);
            membershipRequest.setGroupId(communityId);
            membershipRequest.setUserId(currentUserId);
            membershipRequest.setCreateDate(new Date());
            // TODO: add comment field?
            membershipRequest.setComments("");

            MembershipRequestLocalServiceUtil
                    .addMembershipRequest(membershipRequest);

            // TODO: thing about notification (email, etc)
            // ServiceContext serviceContext = new ServiceContext();
            // serviceContext.setScopeGroupId(communityId);
            //
            //
            // MembershipRequestLocalServiceUtil.addMembershipRequest(currentUserId,
            // communityId, "", serviceContext);

        } catch (SystemException e) {
            e.printStackTrace();
        }
        // catch (PortalException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

    }

    /**
     * Get all groups from type "public"
     * 
     * @return list of groups
     */
    private List<Group> getAllGroupsFromCompanyId(long companyId) {

        DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
                Group.class, PortalClassLoaderUtil.getClassLoader());

        // only groups from actual company
        dynamicQuery.add(RestrictionsFactoryUtil.eq("companyId", companyId));

        dynamicQuery.add(RestrictionsFactoryUtil.eq("classNameId",
                PortalUtil.getClassNameId(Group.class)));

        // but not the guest community
        dynamicQuery.add(RestrictionsFactoryUtil.ne("name",
                GroupConstants.GUEST));
        // and not the control panel group
        dynamicQuery.add(RestrictionsFactoryUtil.ne("name",
                GroupConstants.CONTROL_PANEL));

        // reverse order (newest groups first)
        dynamicQuery.addOrder(OrderFactoryUtil.desc("groupId"));
        try {
            return GroupLocalServiceUtil.dynamicQuery(dynamicQuery);
        } catch (SystemException e) {
            _log.error("error while trying to get public communities: " + e);
            return null;
        }

    }

    /**
     * get an image id per group for image with name "logo"
     * 
     * @param companyId
     *            company filter with id
     * @param currentPublicGroups
     *            list of groups
     * @return hash map with group id to image id
     * @throws SystemException
     */
    private HashMap<Long, Long> getImageFolderIdsToGroupIdsWithNameLogoFromCompany(
            long companyId, List<Group> currentPublicGroups) {

        HashMap<Long, Long> returnImageFolderIdsToGroupIds = new HashMap<Long, Long>();

        DynamicQuery dynamicQuery = DynamicQueryFactoryUtil.forClass(
                DLFileEntry.class, PortalClassLoaderUtil.getClassLoader());

        // only from company
        dynamicQuery.add(RestrictionsFactoryUtil.eq("companyId", companyId));

        dynamicQuery.add(RestrictionsFactoryUtil.eq("title", "LOGO"));
        List<DLFileEntry> tmpImagesWithNameLogoFromCompany;
        try {
            tmpImagesWithNameLogoFromCompany = GroupLocalServiceUtil
                    .dynamicQuery(dynamicQuery);
            // create hash map with <groupId, imageId>
            for (DLFileEntry igImage : tmpImagesWithNameLogoFromCompany) {
                if (!returnImageFolderIdsToGroupIds.containsKey(igImage
                        .getGroupId())) {
                    returnImageFolderIdsToGroupIds.put(igImage.getGroupId(),
                            igImage.getFolderId());
                }
            }
        } catch (SystemException e) {
            _log.error("error while trying to get image ids from group ids :"
                    + e);
        }

        return returnImageFolderIdsToGroupIds;
    }

    /**
     * get number of members from group as list by a list of groups
     * 
     * @param currentPublicGroups
     *            list if groups
     * @return hash table with groupId to integer (number of members)
     */
    private HashMap<Long, Integer> getNumberOfMembersFromCommunityById(
            List<Group> currentPublicGroups) {

        HashMap<Long, Integer> returnGroupIdsToMembersCount = new HashMap<Long, Integer>();

        // fill hashmap with <groupId, Members count>
        for (Group group : currentPublicGroups) {
            try {
                returnGroupIdsToMembersCount.put(group.getGroupId(),
                        UserLocalServiceUtil.getGroupUsersCount(group
                                .getGroupId()));
            } catch (SystemException e) {
                _log.error("error while trying to get put number of members in hash map: "
                        + e);
                e.printStackTrace();
            }
        }

        return returnGroupIdsToMembersCount;

    }
    

}