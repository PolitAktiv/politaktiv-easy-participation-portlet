package org.politaktiv.easyParticipation.application;

import java.util.List;
import java.util.Set;

import org.politaktiv.community.application.CommunityView;
import org.politaktiv.community.application.CommunityViewContainer;
import org.politaktiv.community.application.InitializeEvent;
import org.politaktiv.community.application.JoinEvent;
import org.politaktiv.community.application.LeaveEvent;
import org.politaktiv.community.application.RequestMembershipEvent;
import org.politaktiv.community.application.SearchEvent;
import org.politaktiv.community.domain.CommunitiesRepository;
import org.politaktiv.community.domain.Community;
import org.politaktiv.community.domain.CommunityService;
import org.politaktiv.community.domain.MembershipRequestService;
import org.politaktiv.community.domain.PortalState;

import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.util.PortalUtil;

public class CommunityServiceImpl implements CommunityService{

    static final String COMMUNITY_DOMAIN_LIST = "COMMUNITY_DOMAIN_LIST";
    CommunitiesRepository repository = new CommunitiesRepositoryImpl();    
    MembershipRequestService membershipRequestService= new MembershipRequestServiceImpl();
    int showOtherLimit = 10;

    public void setCommunitiesRepository(CommunitiesRepository repository) {
    this.repository = repository;
    }



    public void setShowOtherLimit(int showOtherLimit) {
    this.showOtherLimit = showOtherLimit;
    }

    public CommunityViewContainer initializeView(InitializeEvent initializeEvent){
    CommunityViewContainer container = new CommunityViewContainer(initializeEvent.getCurrentCompanyId(), "",
        initializeEvent.getPortalState());

    container = searchCommunity(container, container.getNameToSearch());

    return container;
    }

    public CommunityViewContainer searchCommunity(CommunityViewContainer container, SearchEvent searchEvent) {
    if (container.isDirty(searchEvent.getPortalState())
        || !container.getNameToSearch().equals(searchEvent.getNameToSearch())) {
        container.setPortalState(searchEvent.getPortalState());
        container.setNameToSearch(searchEvent.getNameToSearch());
        container = searchCommunity(container, searchEvent.getNameToSearch());
    }
    return container;
    }

    public CommunityViewContainer calculateView(CommunityViewContainer container, PortalState currentPortalState) {
    if (container.isDirty(currentPortalState)) {
        container.setPortalState(currentPortalState);
        container = searchCommunity(container, container.getNameToSearch());
    }

    return container;
    }

    CommunityViewContainer searchCommunity(CommunityViewContainer container, String nameToSearch){

    PortalState portalState = container.getPortalState();
    container.resetResults();

    List<Community> communityDomainList;
    if (nameToSearch.isEmpty()) {
        communityDomainList = repository.findCommunitiesByCompanyId(container.getCurrentCompanyId());
    } else {
        communityDomainList = repository.findCommunitiesByCompanyIdAndSearchString(container.getCurrentCompanyId(),
            nameToSearch);
    }

    Set<Long> userGroupIds = null;
    long userId = 0;
    if (portalState.isSignedIn()) {
        userId = portalState.getUserId();
        userGroupIds = portalState.getGroupIds();
    }

    for (Community communityDomain : communityDomainList) {
        if (portalState.isSignedIn()) {
        container = handleSignedInCase(container, portalState, userGroupIds, userId, communityDomain);
        } else {
        container = handleSignedOffCase(container, portalState, userId, communityDomain);
        }
    }

    return container;
    }

    CommunityViewContainer handleSignedInCase(CommunityViewContainer container, PortalState portalState,
        Set<Long> userGroupIds, long userId, Community communityDomain) {
    boolean isGroupMember = isGroupMember(userGroupIds, communityDomain);
    if (isGroupMember) {
        container.addMemberCommunity(createMemberCommunity(portalState, communityDomain));
    } else if (communityDomain.isOpenCommunity()) {
        container.addNonMemberOpenCommunity(createOpenCommunity(portalState, communityDomain, isGroupMember));
    }

    if (communityDomain.isOpenCommunity()) {
        container.addOpenCommunity(createOpenCommunity(portalState, communityDomain, isGroupMember));
    } else if (communityDomain.isRestrictedCommunity()) {
        container.addRestrictedCommunity(createOtherRestrictedCommuity(userId, portalState, communityDomain,
            isGroupMember));
    }
    return container;
    }

    CommunityViewContainer handleSignedOffCase(CommunityViewContainer container, PortalState portalState, long userId,
        Community communityDomain){
    if (communityDomain.isOpenCommunity()) {
        container.addOpenCommunity(createOpenCommunity(portalState, communityDomain, false));
        container.addNonMemberOpenCommunity(createOpenCommunity(portalState, communityDomain, false));
    } else if (communityDomain.isRestrictedCommunity()) {
        container.addRestrictedCommunity(createOtherRestrictedCommuity(userId, portalState, communityDomain, false));
    }

    return container;
    }

    CommunityView createMemberCommunity(PortalState portalState, Community communityDomain) {

    String actionIcon = "leave";
    String actionText = "give-up";
    String action = "LEAVE";

    String urlToCommunity = calculateUrlToCommunity(portalState, communityDomain.getFriendlyUrl());
    String urlToLogo = calculateUrlToLogo(communityDomain);

    CommunityView communityView = new CommunityView(communityDomain.getName(), Long.toString(communityDomain
        .getCommunityId()), urlToCommunity, urlToLogo, communityDomain.getMemberCount(), actionIcon,
        actionText, action);

    return communityView;
    }

    CommunityView createOpenCommunity(PortalState portalState, Community communityDomain, boolean isMember) {

    String actionIcon;
    String actionText;
    String action;

    if (portalState.isSignedIn()) {
        if (isMember) {
        actionIcon = "leave";
        actionText = "give-up";
        action = "LEAVE";
        } else {
        actionIcon = "join";
        actionText = "participate";
        action = "JOIN";
        }
    } else {
        actionIcon = "status_online";
        actionText = "please-log-in";
        action = "LOGIN";
    }

    String urlToCommunity = calculateUrlToCommunity(portalState, communityDomain.getFriendlyUrl());
    String urlToLogo = calculateUrlToLogo(communityDomain);

    CommunityView communityView = new CommunityView(communityDomain.getName(), Long.toString(communityDomain
        .getCommunityId()), urlToCommunity, urlToLogo, communityDomain.getMemberCount(), actionIcon,
        actionText, action);

    return communityView;
    }

    CommunityView createOtherRestrictedCommuity(long userId, PortalState portalState, Community communityDomain,
        boolean isMember) {

    String actionIcon;
    String actionText;
    String action;

    if (portalState.isSignedIn()) {
        
        boolean isUserMembershipRequestPending = false;
        try {
            isUserMembershipRequestPending = membershipRequestService.isUserMembershipRequestPending(
                userId, communityDomain.getCommunityId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isMember) {
        actionIcon = "leave";
        actionText = "give-up";
        action = "LEAVE";
        } else if (isUserMembershipRequestPending) {
        actionIcon = "checked";
        actionText = "membership-requested";
        action = "JOIN";
        } else {
        actionIcon = "join";
        actionText = "request-membership";
        action = "JOIN";
        }
    } else {
        actionIcon = "status_online";
        actionText = "please-log-in";
        action = "LOGIN";
    }

    String urlToCommunity = calculateUrlToCommunity(portalState, communityDomain.getFriendlyUrl());
    String urlToLogo = calculateUrlToLogo(communityDomain);

    CommunityView communityView = new CommunityView(communityDomain.getName(), Long.toString(communityDomain
        .getCommunityId()), urlToCommunity, urlToLogo, communityDomain.getMemberCount(), actionIcon,
        actionText, action);
    return communityView;
    }

    String calculateUrlToLogo(Community communityDomain) {
    String urlToLogo;
    if (communityDomain.hasLogo()) {
        urlToLogo = "/documents/" + communityDomain.getCommunityId() + "/" + communityDomain.getLogoFolderId()
            + "/LOGO?imageThumbnail=1";
    } else {
        urlToLogo = "/images/building.png";
    }
    return urlToLogo;
    }

    String calculateUrlToCommunity(PortalState portalState, String communityUrl) {
    String urlToCommunity = portalState.getPortalUrl() + (portalState.isI18n() ? portalState.getI18nPath() : "")
        + PortalUtil.getPathFriendlyURLPublic() + communityUrl;
    if (null != portalState.getDoAsUserId() && !"".equals(portalState.getDoAsUserId())) {
        urlToCommunity = HttpUtil.addParameter(urlToCommunity, "doAsUserId", portalState.getDoAsUserId());
    }
    return urlToCommunity;
    }

    boolean isGroupMember(Set<Long> userGroupIds, Community communityDomain) {
    return userGroupIds.contains(communityDomain.getCommunityId());
    }

    public CommunityViewContainer joinCommunity(CommunityViewContainer container, JoinEvent event){
    repository.joinCommunity(event.getUserId(), event.getCommunityId());
    PortalState newPortalState = event.getPortalState();
    newPortalState.addGroupId(event.getCommunityId());
    container.setPortalState(newPortalState);
    container = searchCommunity(container, container.getNameToSearch());
    return container;
    }
    
    public CommunityViewContainer leaveCommunity(CommunityViewContainer container, LeaveEvent event){
    repository.leaveCommunity(event.getUserId(), event.getCommunityId());
    PortalState newPortalState = event.getPortalState();
    newPortalState.removeGroupId(event.getCommunityId());
    container.setPortalState(newPortalState);
    container = searchCommunity(container, container.getNameToSearch());
    return container;
    }

    public CommunityViewContainer requestCommunityMembership(CommunityViewContainer container,
        RequestMembershipEvent event){
    try {
        repository.requestMembershipToCommunity(event.getUserId(), event.getCompanyId(), event.getCommunityId(),
            event.getGuestUserId());
    } catch (Exception e) {
        e.printStackTrace();
    }
    container.setPortalState(event.getPortalState());
    container = searchCommunity(container, container.getNameToSearch());
    return container;
    }



    public void setMembershipRequestService(
            MembershipRequestService membershipRequestService) {
        this.membershipRequestService = membershipRequestService;
        
    }


}
