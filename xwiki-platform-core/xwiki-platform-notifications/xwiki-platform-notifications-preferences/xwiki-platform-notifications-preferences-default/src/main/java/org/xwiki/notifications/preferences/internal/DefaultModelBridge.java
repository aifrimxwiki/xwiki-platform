/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.notifications.preferences.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link ModelBridge}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    private static final String EVENT_TYPE_FIELD = "eventType";

    private static final String APPLICATION_ID_FIELD = "applicationId";

    private static final String IS_WATCHLIST_FIELD = "isWatchlist";

    private static final String START_DATE_FIELD = "startDate";

    private static final String NOTIFICATION_ENABLED_FIELD = "notificationEnabled";

    private static final String FORMAT_FIELD = "format";

    private static final SpaceReference NOTIFICATION_CODE_SPACE = new SpaceReference("Code",
        new SpaceReference("Notifications",
            new SpaceReference("XWiki", new WikiReference("xwiki"))
        )
    );

    private static final DocumentReference NOTIFICATION_PREFERENCE_CLASS = new DocumentReference(
            "NotificationPreferenceClass", NOTIFICATION_CODE_SPACE
    );

    private static final String NOTIFICATION_START_DATE_UPDATE_COMMENT = "Update start date for the notifications.";

    private static final String SET_USER_START_DATE_ERROR_MESSAGE = "Failed to set the user start date for [%s].";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private TargetableNotificationPreferenceBuilder notificationPreferenceBuilder;

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference)
            throws NotificationException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        final DocumentReference notificationPreferencesClass
                = NOTIFICATION_PREFERENCE_CLASS.setWikiReference(userReference.getWikiReference());

        List<NotificationPreference> preferences = new ArrayList<>();

        try {
            XWikiDocument doc = xwiki.getDocument(userReference, context);
            List<BaseObject> preferencesObj = doc.getXObjects(notificationPreferencesClass);
            if (preferencesObj != null) {
                for (BaseObject obj : preferencesObj) {
                    if (obj != null) {
                        String objFormat = obj.getStringValue(FORMAT_FIELD);
                        Date objStartDate = obj.getDateValue(START_DATE_FIELD);
                        int objIsWatchlist = obj.getIntValue(IS_WATCHLIST_FIELD, 0);

                        Map<NotificationPreferenceProperty, Object> properties =
                                extractNotificationPreferenceProperties(obj);

                        notificationPreferenceBuilder.prepare();
                        notificationPreferenceBuilder.setProperties(properties);
                        notificationPreferenceBuilder.setStartDate(
                                (objStartDate != null) ? objStartDate : doc.getCreationDate());
                        notificationPreferenceBuilder.setFormat(StringUtils.isNotBlank(objFormat)
                                    ? NotificationFormat.valueOf(objFormat.toUpperCase())
                                    : NotificationFormat.ALERT);
                        notificationPreferenceBuilder.setTarget(userReference);
                        notificationPreferenceBuilder.setEnabled(
                                obj.getIntValue(NOTIFICATION_ENABLED_FIELD, 0) != 0);
                        notificationPreferenceBuilder.setCategory((objIsWatchlist == 0)
                                ? NotificationPreferenceCategory.DEFAULT
                                : NotificationPreferenceCategory.WATCHLIST);

                        preferences.add(notificationPreferenceBuilder.build());
                    }
                }
            }
        } catch (Exception e) {
            throw new NotificationException(
                    String.format("Failed to get the notification preferences for the user [%s].",
                            userReference), e);
        }

        return preferences;
    }

    private Map<NotificationPreferenceProperty, Object> extractNotificationPreferenceProperties(BaseObject object)
    {
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();

        String eventType = object.getStringValue(EVENT_TYPE_FIELD);
        if (StringUtils.isNotBlank(eventType)) {
            properties.put(NotificationPreferenceProperty.EVENT_TYPE, eventType);
        }

        String applicationId = object.getStringValue(APPLICATION_ID_FIELD);
        if (StringUtils.isNotBlank(applicationId)) {
            properties.put(NotificationPreferenceProperty.APPLICATION_ID, applicationId);
        }

        return properties;
    }

    @Override
    public void setStartDateForUser(DocumentReference userReference, Date startDate)
            throws NotificationException
    {
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument document =  xwiki.getDocument(userReference, context);

            List<BaseObject> objects = document.getXObjects(NOTIFICATION_PREFERENCE_CLASS);
            if (objects != null) {
                for (BaseObject object : objects) {
                    if (object != null) {
                        object.setDateValue(START_DATE_FIELD, startDate);
                    }
                }
            }

            xwiki.saveDocument(document, NOTIFICATION_START_DATE_UPDATE_COMMENT, context);

        } catch (Exception e) {
            throw new NotificationException(
                    String.format(SET_USER_START_DATE_ERROR_MESSAGE, userReference), e);
        }
    }

    /**
     * Search for the XObject corresponding to the given notification preference in the given document. If no object
     * is found, returns null.
     *
     * @param xWikiDocument the document to search in
     * @param notificationPreference the notification preference
     * @return the corresponding NotificationPreferences XObject
     * @throws NotificationException if the base object could not be found
     * @since 9.7RC1
     */
    private BaseObject findNotificationPreference(XWikiDocument xWikiDocument,
            NotificationPreference notificationPreference) throws NotificationException
    {
        List<BaseObject> objects = xWikiDocument.getXObjects(NOTIFICATION_PREFERENCE_CLASS);

        if (objects != null) {
            for (BaseObject object : objects) {
                if (object != null
                        && notificationPreference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)
                        && notificationPreference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)
                            .equals(object.getStringValue(EVENT_TYPE_FIELD))) {
                    String format = object.getStringValue(FORMAT_FIELD);

                    // Ensure that we have the correct notification format
                    if ((StringUtils.isBlank(format) && notificationPreference.getFormat().equals(
                            NotificationFormat.ALERT))
                            || notificationPreference.getFormat().equals(NotificationFormat.valueOf(
                                    format.toUpperCase()))) {
                        return object;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void saveNotificationsPreferences(DocumentReference userReference,
            List<NotificationPreference> notificationPreferences) throws NotificationException
    {
        try {
            XWikiContext context = contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument document =  xwiki.getDocument(userReference, context);

            for (NotificationPreference notificationPreference : notificationPreferences) {

                // Ensure that the notification preference has an event type to save
                if (!notificationPreference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)) {
                    continue;
                }

                // Try to find the corresponding XObject for the notification preference
                BaseObject preferenceObject = this.findNotificationPreference(document, notificationPreference);

                // If no preference exist, then create one
                if (preferenceObject == null) {
                    preferenceObject = new BaseObject();
                    preferenceObject.setXClassReference(NOTIFICATION_PREFERENCE_CLASS);
                    document.addXObject(preferenceObject);
                }

                preferenceObject.set(EVENT_TYPE_FIELD,
                        notificationPreference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE), context);
                preferenceObject.set(FORMAT_FIELD, notificationPreference.getFormat().name().toLowerCase(),
                        context);
                preferenceObject.set(NOTIFICATION_ENABLED_FIELD,
                        (notificationPreference.isNotificationEnabled() ? 1 : 0), context);

                Date startDate = null;
                if (notificationPreference.isNotificationEnabled()) {
                    startDate = notificationPreference.getStartDate();
                    if (startDate == null) {
                        // Fallback to the previous value if date is empty
                        startDate = preferenceObject.getDateValue(START_DATE_FIELD);
                        if (startDate == null) {
                            // Fallback to now
                            startDate = new Date();
                        }
                    }
                }
                preferenceObject.set(START_DATE_FIELD, startDate, context);
            }

            xwiki.saveDocument(document, "Update notification preferences.", context);

        } catch (XWikiException e) {
            throw new NotificationException(String.format(
                    "Failed to save the notification preference for [%s]", userReference));
        }
    }
}
