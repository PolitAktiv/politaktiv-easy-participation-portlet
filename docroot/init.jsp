<%--
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
        
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 --%>
 

<%@taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %> 

<%@page import="java.util.ResourceBundle" %>
<%@page import="java.util.Locale" %>
<%@page import="com.liferay.portal.service.GroupServiceUtil"%>
<%@page import="com.liferay.portal.service.GroupLocalServiceUtil"%>
<%@page import="com.liferay.portal.util.PortalUtil"%>
<%@page import="com.liferay.portal.model.GroupConstants"%>
<%@page import="org.politaktiv.community.domain.Community"%>
<%@page import="java.util.List"%>
<%@page import="com.liferay.portal.kernel.util.WebKeys"%>
<%@page import="com.liferay.portal.theme.ThemeDisplay"%>
<%@page import="com.liferay.portal.model.MembershipRequestConstants" %>
<%@page import="com.liferay.portal.kernel.language.LanguageUtil" %>
<%@page import="com.liferay.portal.kernel.language.UnicodeLanguageUtil" %>
<%@page import="org.politaktiv.community.application.CommunityView" %>
<%@page import="org.politaktiv.community.application.CommunityViewContainer" %>
<%@page import="org.politaktiv.community.application.CommunityViewConstants" %>
<%@page import="com.liferay.portal.kernel.util.Constants" %>
<%@page import="org.politaktiv.easyParticipation.application.MembershipRequestServiceImpl"%>
<%@page import="java.util.Set"%>
<%@page import="com.liferay.portal.model.Group"%>
<%@page import="com.liferay.portal.model.User"%>
<%@page import="org.politaktiv.community.application.CommunitySerializationUtil"%>
