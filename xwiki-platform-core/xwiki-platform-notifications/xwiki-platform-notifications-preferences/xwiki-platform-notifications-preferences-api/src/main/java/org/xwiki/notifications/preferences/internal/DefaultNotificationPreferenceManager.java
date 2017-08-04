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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProvider;

/**
 * This is the default implementation of {@link NotificationPreferenceManager}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
public class DefaultNotificationPreferenceManager implements NotificationPreferenceManager
{
    @Inject
    @Named("cached")
    private ModelBridge cachedModelBridge;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference user)
            throws NotificationException
    {
        try {
            // Get every registered notification provider
            List<NotificationPreferenceProvider> providers =
                    componentManager.getInstanceList(NotificationPreferenceProvider.class);

            List<NotificationPreference> notificationPreferences = new ArrayList<>();

            /**
             * TODO: Handle notification preferences conflicts.
             * That’s why {@link NotificationPreferenceProvider#getProviderPriority()} exists.
             */
            for (NotificationPreferenceProvider provider : providers) {
                notificationPreferences.addAll(provider.getPreferencesForUser(user));
            }

            return notificationPreferences;
        } catch (ComponentLookupException e) {
            // Don’t include the DocumentReference parameter here as it’s not relevant
            throw new NotificationException(
                    "Unable to fetch the notifications preferences providers from the component manager", e);
        }

    }

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference user, boolean isEnabled,
            NotificationFormat format) throws NotificationException
    {
        List<NotificationPreference> preferences = getNotificationsPreferences(user);

        Iterator<NotificationPreference> it = preferences.iterator();
        while (it.hasNext()) {
            NotificationPreference preference = it.next();

            if (preference.isNotificationEnabled() != isEnabled
                || !preference.getFormat().equals(format)) {
                it.remove();
            }
        }

        return preferences;
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        cachedModelBridge.setStartDateForUser(user, startDate);
    }

    @Override
    public void saveNotificationsPreferences(List<NotificationPreference> notificationPreferences)
            throws NotificationException
    {
        // We construct a map where each key is a provider hint and each value is a list of associated providers
        // this allows calling each provider only once
        Map<String, List<NotificationPreference>> preferencesMapping = new HashMap<>();

        for (NotificationPreference notificationPreference : notificationPreferences) {
            // Try to get the corresponding provider, if no provider can be found, discard the save of the preference
            String providerHint = notificationPreference.getProviderHint();
            if (componentManager.hasComponent(NotificationPreferenceProvider.class, providerHint)) {

                if (!preferencesMapping.containsKey(providerHint)) {
                    preferencesMapping.put(providerHint, new ArrayList<>());
                }

                preferencesMapping.get(providerHint).add(notificationPreference);
            }
        }

        // Once we have created the mapping, save all the preferences using their correct providers
        for (String providerHint : preferencesMapping.keySet()) {
            try {
                NotificationPreferenceProvider provider =
                        componentManager.getInstance(NotificationPreferenceProvider.class, providerHint);

                provider.savePreferences(preferencesMapping.get(providerHint));

            } catch (ComponentLookupException e) {
                logger.error("Unable to retrieve the notification preference provide for hint {}: {}",
                        providerHint, e);
            }
        }
    }
}
