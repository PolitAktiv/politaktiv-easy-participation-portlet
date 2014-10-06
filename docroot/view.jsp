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
	    //get id of the community the user visites at the moment
			ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
			User user = themeDisplay.getUser();
			long currentCommunityId = themeDisplay.getScopeGroupId();
			String currentCommunityName = themeDisplay.getScopeGroupName();
			List<Group> currentUserGroups = user.getGroups();
			
			//Decide whether Button should be rendered:
			boolean showButton = true;
			
			//don't show button, if User isn't logged in
			if(!themeDisplay.isSignedIn()){
			    showButton = false;
			}
			
			//don't show button, if User is already member of current Group
			if(currentUserGroups.contains(themeDisplay.getScopeGroup())){
			    showButton = false;
			}
				
			//don't show button, if user is on those pages that belong to Politaktiv itself
			if(currentCommunityName.equals(GroupConstants.GUEST)){
			    showButton = false;
			}
			
			//don't show button, if user is on those pages that belong to the control panel
			if(currentCommunityName.equals(GroupConstants.CONTROL_PANEL)){
			    showButton = false;
			}
			//don't show the button, if a membership request is already pending
			if(new MembershipRequestServiceImpl().isUserMembershipRequestPending(themeDisplay.getUserId(), currentCommunityId)){
			    showButton = false;
			}
				
			//Find out whether the current page is a restricted community
			byte[] serializedContainer = (byte[])(renderRequest.getAttribute(CommunityViewConstants.COMMUNITY_VIEW));
			CommunityViewContainer viewContainer = (CommunityViewContainer) CommunitySerializationUtil.deserializeContainer(serializedContainer); 
			
			boolean isCurrentCommunityRestricted = false;
			for(CommunityView communityView :  viewContainer.getRestrictedCommunities()){
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
	
	<%
	// set button contents
	String buttonValue = 
	        (isCurrentCommunityRestricted) ? LanguageUtil.get(pageContext, "Request-Membership") : LanguageUtil.get(pageContext, "Participate");
	
	String buttonHelpText =
	        (isCurrentCommunityRestricted) ? LanguageUtil.format(pageContext, "request-help-text-x", currentCommunityName) : LanguageUtil.format(pageContext, "participate-help-text-x", currentCommunityName);
	%>

	
	<% if(showButton){ %>
		<div id="participationButtonContainer" >	
			<aui:form action="<%=actionURL%>">
		        <aui:button
		        	name="participationButton" 
		        	help-text-tooltip = "<%=buttonHelpText %>"
		        	type="submit" 
		        	value="<%=buttonValue%>">	
		        </aui:button>      
			</aui:form>
		</div>
	<% } %>

	

	

	
	