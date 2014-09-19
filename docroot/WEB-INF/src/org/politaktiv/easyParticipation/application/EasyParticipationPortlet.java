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

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.politaktiv.community.application.CommunityActionConstants;
import org.politaktiv.community.application.CommunityService;
import org.politaktiv.community.application.CommunityViewConstants;
import org.politaktiv.community.application.CommunityViewContainer;
import org.politaktiv.community.application.Event;
import org.politaktiv.community.application.InitializeEvent;
import org.politaktiv.community.application.JoinEvent;
import org.politaktiv.community.application.LeaveEvent;
import org.politaktiv.community.application.MembershipRequestService;
import org.politaktiv.community.application.RequestMembershipEvent;
import org.politaktiv.community.application.SearchEvent;
import org.politaktiv.community.domain.PortalState;
import org.politaktiv.infrastructure.liferay.PAParamUtil;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.User;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.util.bridges.mvc.MVCPortlet;

/**
 * Portlet implementation class CommunitiesPortlet extends model view controler
 * portlet from liferay
 */
public class EasyParticipationPortlet extends MVCPortlet {
    private static final String EVENT_QUEUE = "EVENT_LIST";
    private static Log _log = LogFactoryUtil.getLog(EasyParticipationPortlet.class);
    CommunityService communityService = new CommunityServiceImpl();
    PAParamUtil PaParamUtil = new PAParamUtil();
    EasyParticipationPortletUtil participationUtil = new EasyParticipationPortletUtil();
    PortalState currentPortalState;
    MembershipRequestService membershiRequestService = new MembershipRequestServiceImpl();

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.liferay.util.bridges.mvc.MVCPortlet#doView(javax.portlet.RenderRequest
     * , javax.portlet.RenderResponse)
     */
    @Override
    public void doView(RenderRequest renderRequest,
            RenderResponse renderResponse) throws IOException, PortletException {

        ThemeDisplay themeDisplay = getThemeDisplay(renderRequest);
        User user = themeDisplay.getUser();
        PortletSession portletSession = renderRequest.getPortletSession();
        if(currentPortalState == null){
            currentPortalState = new PortalState(
                    themeDisplay.isSignedIn(), 
                    themeDisplay.getPortalURL(),
                    themeDisplay.isI18n(), 
                    themeDisplay.getI18nPath(),
                    themeDisplay.getDoAsUserId(), 
                    user.getUserId(),
                    participationUtil.initializeUserGroupSet(user));
        }



        try {
            portletSession = doLazyInitializeSession(portletSession,
                    themeDisplay.getCompanyId(), currentPortalState);
            //consume Events of User Interaction
            portletSession = consumeEvents(portletSession, currentPortalState);
          //send system generated Events to hold the data consistent
            adjustPortalState(themeDisplay, portletSession);
            //consume Events of System that were fired because the group membership of the user changed 
            //(most likely because of the CommunitySelectPortlet)
            portletSession = consumeEvents(portletSession, currentPortalState);
            renderRequest = copyViewFromSessionToRequest(renderRequest,
                    portletSession);

        } catch (Exception e) {
            throw new PortletException(e);
        }

        super.doView(renderRequest, renderResponse);
    }

    /**
     * Join the current user (scope) to community with given community id, if
     * user is allowed (function call comes from actionURL)
     * 
     * @param actionRequest
     * @param actionResponse
     */
    public void joinCommunity(ActionRequest actionRequest,
            ActionResponse actionResponse) {
        PortletSession portletSession = actionRequest.getPortletSession();
        ThemeDisplay themeDisplay = getThemeDisplay(actionRequest);
        User user = themeDisplay.getUser();
        long currentUserId = user.getUserId();
        long communityId = Long.parseLong(actionRequest
                .getParameter(CommunityViewConstants.COMMUNITY_ID));
        currentPortalState = new PortalState(
                themeDisplay.isSignedIn(), 
                themeDisplay.getPortalURL(),
                themeDisplay.isI18n(), 
                themeDisplay.getI18nPath(),
                themeDisplay.getDoAsUserId(), 
                currentUserId,
                participationUtil.initializeUserGroupSet(user, CommunityActionConstants.JOIN, communityId));

        JoinEvent joinEvent = new JoinEvent(currentUserId, communityId,
                currentPortalState);
        fireEvent(portletSession, joinEvent);
    }

    public void requestMembershipToCommunity(ActionRequest actionRequest,
            ActionResponse actionResponse) throws PortalException,
            SystemException {
        PortletSession portletSession = actionRequest.getPortletSession();
        ThemeDisplay themeDisplay = getThemeDisplay(actionRequest);
        User user = themeDisplay.getUser();
        long currentUserId = user.getUserId();
        long communityId = Long.parseLong(actionRequest
                .getParameter(CommunityViewConstants.COMMUNITY_ID));

        long currentGuestUserId = themeDisplay.getDefaultUserId();
        long currentCompanyId = themeDisplay.getCompanyId();

        RequestMembershipEvent rmEvent = new RequestMembershipEvent(
                currentUserId, currentCompanyId, communityId,
                currentGuestUserId, currentPortalState);
        fireEvent(portletSession, rmEvent);
    }

    // dispatch helper for portlet to redirect to specific page
    @Override
    protected void include(String path, RenderRequest renderRequest,
            RenderResponse renderResponse) throws IOException, PortletException {
        PortletRequestDispatcher portletRequestDispatcher = getPortletContext()
                .getRequestDispatcher(path);
        if (portletRequestDispatcher == null) {
            _log.error(path + " is not a valid include");
        } else {
            portletRequestDispatcher.include(renderRequest, renderResponse);
        }
    }

    PortletSession doLazyInitializeSession(PortletSession portletSession,
            long currentCompanyId, PortalState currentPortalState) {
        CommunityViewContainer containerToShow = getViewContainer(portletSession);
        if (null == containerToShow) {
            Event event = new InitializeEvent(currentCompanyId,
                    currentPortalState);
            portletSession = fireEvent(portletSession, event);
        }
        return portletSession;
    }

    PortletSession consumeEvents(PortletSession portletSession,
            PortalState currentPortalState) throws PortalException,
            SystemException {
        Queue<Event> eventQueue = getOrCreateEventListFromSession(portletSession);
        CommunityViewContainer container = getViewContainer(portletSession);

        while (!eventQueue.isEmpty()) {
            Event event = eventQueue.poll();
            if (event instanceof InitializeEvent) {
                container = communityService
                        .initializeView((InitializeEvent) event);
            } else if (event instanceof SearchEvent) {
                container = communityService.searchCommunity(container,
                        (SearchEvent) event);
            } else if (event instanceof JoinEvent) {
                container = communityService.joinCommunity(container,
                        (JoinEvent) event);
            } else if (event instanceof LeaveEvent) {
                container = communityService.leaveCommunity(container,
                        (LeaveEvent) event);
            } else if (event instanceof RequestMembershipEvent) {
                container = communityService.requestCommunityMembership(
                        container, (RequestMembershipEvent) event);
            } else {
                container = communityService.calculateView(container,
                        currentPortalState);
            }
        }
        eventQueue.clear();
        portletSession = putViewContainer(portletSession, container);
        return portletSession;
    }

    <T extends PortletRequest> T copyViewFromSessionToRequest(T request,
            PortletSession portletSession) {
        CommunityViewContainer containerToShow = getViewContainer(portletSession);
        request.setAttribute(CommunityViewConstants.COMMUNITY_VIEW,
                containerToShow);
        request.setAttribute(CommunityViewConstants.SEARCH_STRING,
                containerToShow.getNameToSearch());
        return request;
    }

    PortletSession fireEvent(PortletSession portletSession, Event event) {
        Queue<Event> eventQueue = getOrCreateEventListFromSession(portletSession);
        eventQueue.offer(event);
        return portletSession;
    }

    ThemeDisplay getThemeDisplay(PortletRequest portletRequest) {
        Object attribute = portletRequest.getAttribute(WebKeys.THEME_DISPLAY);
        assert (null != attribute && attribute instanceof ThemeDisplay);
        ThemeDisplay themeDisplay = (ThemeDisplay) attribute;
        return themeDisplay;
    }

    PortletSession putViewContainer(PortletSession portletSession,
            CommunityViewContainer containerToShow) {
        portletSession.setAttribute(CommunityViewConstants.COMMUNITY_VIEW,
                containerToShow, PortletSession.APPLICATION_SCOPE);
        return portletSession;
    }

    CommunityViewContainer getViewContainer(PortletSession portletSession) {
        CommunityViewContainer containerToShow = (CommunityViewContainer) portletSession
                .getAttribute(CommunityViewConstants.COMMUNITY_VIEW,
                        PortletSession.APPLICATION_SCOPE);
        return containerToShow;
    }

    @SuppressWarnings("unchecked")
    Queue<Event> getOrCreateEventListFromSession(PortletSession portletSession) {
        Object attribute = portletSession.getAttribute(EVENT_QUEUE,
                PortletSession.APPLICATION_SCOPE);
        if (attribute == null) {
            attribute = new ArrayBlockingQueue<Event>(2);
            portletSession.setAttribute(EVENT_QUEUE, attribute,
                    PortletSession.APPLICATION_SCOPE);
        }
        assert (attribute instanceof Queue);
        return (Queue<Event>) attribute;
    }

    
    /**
     *  method that fires system generated join/leave events.
     *  This is necessary to hold the data(currentPortalState) consistent with the whole portal.
     * @throws SystemException 
     */
    public void adjustPortalState(ThemeDisplay themeDisplay, PortletSession portletSession) throws SystemException {
        
        long[] userGroupIdArray = {};
        try {
            userGroupIdArray = themeDisplay.getUser().getGroupIds();
        } catch (SystemException e) {
            e.printStackTrace();
        }
        //Case: in the current portal state are communities missing -> join them
        for(Long userGroupId : userGroupIdArray){
            if(!currentPortalState.getGroupIds().contains(userGroupId)){
                adjustmentJoinCommunity(portletSession, themeDisplay, userGroupId);
                //_log.info("joined: " + userGroupId);
            }
        }
        //Case: there are to much communities in the current portal state -> kick them       
        for(long stateGroupId : currentPortalState.getGroupIds()){
            boolean isContained = false;
            for(Long userGroupId : userGroupIdArray){
                if(userGroupId == stateGroupId){
                    isContained = true;
                }
            }
            if(!isContained){
                adjustmentLeaveCommunity(portletSession, themeDisplay, stateGroupId);
                //_log.info("Kicked: " + stateGroupId);
            }
        }
        
        //Case: A communityRequest was made -> update portal state
        for(Group group : GroupLocalServiceUtil.search(themeDisplay.getCompanyId(), null, null, null, QueryUtil.ALL_POS, QueryUtil.ALL_POS)){
            try {
                if(membershiRequestService.isUserMembershipRequestPending(themeDisplay.getUserId(), group.getGroupId())){
                    adjustmentRequestMembershipToCommunity(portletSession, themeDisplay, group.getGroupId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } 
        }
  
    }


    /**
     * method that joins a community without an action request by the user
     * this exists to adjust the data(currentPortalState) to potential changes made by the user in the communitySelectPortlet
     */
    private void adjustmentJoinCommunity(PortletSession portletSession, ThemeDisplay themeDisplay, long communityId) {
        User user = themeDisplay.getUser();
        long currentUserId = user.getUserId();
        currentPortalState = new PortalState(
                themeDisplay.isSignedIn(), 
                themeDisplay.getPortalURL(),
                themeDisplay.isI18n(), 
                themeDisplay.getI18nPath(),
                themeDisplay.getDoAsUserId(), 
                currentUserId,
                participationUtil.initializeUserGroupSet(user, CommunityActionConstants.JOIN, communityId));
    
        JoinEvent joinEvent = new JoinEvent(currentUserId, communityId,
                currentPortalState);
        fireEvent(portletSession, joinEvent);
    }

    /**
     * method that leaves a community without an action request by the user
     * this exists to adjust the data(currentPortalState) to potential changes made by the user in the communitySelectPortlet
     */
    private void adjustmentLeaveCommunity(PortletSession portletSession, ThemeDisplay themeDisplay, long communityId) {
        User user = themeDisplay.getUser();
        long currentUserId = user.getUserId();
        currentPortalState = new PortalState(
                themeDisplay.isSignedIn(),
                themeDisplay.getPortalURL(),
                themeDisplay.isI18n(),
                themeDisplay.getI18nPath(),
                themeDisplay.getDoAsUserId(),
                currentUserId,
                participationUtil.initializeUserGroupSet(user, CommunityActionConstants.LEAVE, communityId));
    
        LeaveEvent leaveEvent = new LeaveEvent(currentUserId, communityId,
                currentPortalState);
        fireEvent(portletSession, leaveEvent);
    }
    
    
    /**
     * method that sends a requestMembershipEvent to a community without an action request by the user
     * this exists to adjust the data(currentPortalState) to potential changes made by the user in the communitySelectPortlet
     */
    public void adjustmentRequestMembershipToCommunity(PortletSession portletSession, ThemeDisplay themeDisplay, long communityId) throws PortalException,
            SystemException {
        User user = themeDisplay.getUser();
        long currentUserId = user.getUserId();
        long currentGuestUserId = themeDisplay.getDefaultUserId();
        long currentCompanyId = themeDisplay.getCompanyId();

        RequestMembershipEvent rmEvent = new RequestMembershipEvent(
                currentUserId, currentCompanyId, communityId,
                currentGuestUserId, currentPortalState);
        fireEvent(portletSession, rmEvent);
    }
}