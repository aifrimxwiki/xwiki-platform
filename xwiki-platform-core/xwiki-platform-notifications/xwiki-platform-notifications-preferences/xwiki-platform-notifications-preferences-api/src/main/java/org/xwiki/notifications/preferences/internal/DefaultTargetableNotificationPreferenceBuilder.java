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

import java.util.Date;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.NotificationProperty;
import org.xwiki.notifications.preferences.TargetableNotificationPreference;
import org.xwiki.notifications.preferences.TargetableNotificationPreferenceBuilder;

/**
 * This is the default implementation of {@link TargetableNotificationPreferenceBuilder}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
public class DefaultTargetableNotificationPreferenceBuilder implements TargetableNotificationPreferenceBuilder
{
    private TargetablePreference preference;

    private class TargetablePreference extends AbstractNotificationPreference
            implements TargetableNotificationPreference
    {
        private DocumentReference target;

        @Override
        public DocumentReference getTarget()
        {
            return target;
        }
    }

    @Override
    public TargetableNotificationPreference build()
    {
        return preference;
    }

    @Override
    public void prepare()
    {
        preference = new TargetablePreference();
    }

    @Override
    public void setEnabled(boolean isEnabled)
    {
        preference.isNotificationEnabled = isEnabled;
    }

    @Override
    public void setFormat(NotificationFormat format)
    {
        preference.format = format;
    }

    @Override
    public void setProperties(Map<NotificationProperty, Object> properties)
    {
        preference.properties = properties;
    }

    @Override
    public void setProviderHint(String providerHint)
    {
        preference.providerHint = providerHint;
    }

    @Override
    public void setStartDate(Date startDate)
    {
        preference.startDate = startDate;
    }

    @Override
    public void setTarget(DocumentReference target)
    {
        preference.target = target;
    }
}
