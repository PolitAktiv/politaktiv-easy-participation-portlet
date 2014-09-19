
<%@page import="org.politaktiv.easyParticipation.application.MembershipRequestServiceImpl"%>
<%@page import="java.util.Set"%>
<%
/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
%>

<%@ include file="./init.jsp"%>

<portlet:defineObjects />

	<%
	
	//get all communities the user already participates in
    CommunityViewContainer viewContainer = (CommunityViewContainer) renderRequest.getAttribute(CommunityViewConstants.COMMUNITY_VIEW);
	List<CommunityView> viewList;
	viewList = viewContainer.getMemberCommunities();
	
	//get id of the community the user visites at the moment
	ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
	long currentCommunityId = themeDisplay.getScopeGroupId();
	String currentCommunityName = themeDisplay.getScopeGroupName();
	
	//check whether user already participates in current community
	//thus decide whether and how the participate-button should be rendered
	boolean showButton = true;
	for(CommunityView communityView : viewList){
	    if(Long.parseLong(communityView.getId()) == currentCommunityId){
	        showButton = false;
	        break;
	    }
	}
	//don't show button, if user is on those pages that belong to Politaktiv itself
	if(currentCommunityName.equals("PolitAktiv")){
	    showButton = false;
	}
	//don't show the button, if a membership request is already pending
	if(new MembershipRequestServiceImpl().isUserMembershipRequestPending(themeDisplay.getUserId(), currentCommunityId)){
	    showButton = false;
	}
	
	//Find out whether the current page is a restricted community
	boolean isCurrentCommunityRestricted = false;
	for(CommunityView communityView : viewContainer.getRestrictedCommunities()){
	    if(Long.parseLong(communityView.getId()) == currentCommunityId){
	        isCurrentCommunityRestricted = true;
	        break;
	    }
	}
	
	%>

	<!-- generate actionUrls -->
	
	<portlet:actionURL name="requestMembershipToCommunity" var="requestUrl">
		<portlet:param name="action" value="requestMembershipToCommunity" />
		<portlet:param name="<%=CommunityViewConstants.COMMUNITY_ID%>" value="<%=String.valueOf(currentCommunityId)%>" />
	</portlet:actionURL>
	
	<portlet:actionURL name="joinCommunity" var="joinUrl">
		<portlet:param name="action" value="joinCommunity" />
		<portlet:param name="<%=CommunityViewConstants.COMMUNITY_ID%>" value="<%=String.valueOf(currentCommunityId)%>" />
	</portlet:actionURL>
	
	<% 
	// set actionURL
	String actionURL;
	if(isCurrentCommunityRestricted){
	    actionURL = requestUrl.toString();
	}else{
	    actionURL = joinUrl.toString();
	}
	%>
	
	<% if(showButton){ %>
		<aui:form action="<%=actionURL%>">
		        <aui:input name="participationButton" type="submit" value="<%=(isCurrentCommunityRestricted) ? \"Beantragen\" : \"Beitreten\" %>"/>
		</aui:form>
	<% } %>
	
	<br/>
	<%	for(CommunityView communityView : viewList){
	    %>
	    <%= communityView.getName() %>
	    <br/>
	<%
	} %>

