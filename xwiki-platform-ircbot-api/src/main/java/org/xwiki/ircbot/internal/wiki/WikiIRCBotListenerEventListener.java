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
package org.xwiki.ircbot.internal.wiki;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

@Component
@Named("ircbot")
@Singleton
public class WikiIRCBotListenerEventListener implements EventListener
{
    /**
     * Creates {@link WikiIRCBotListener} objects.
     */
    @Inject
    private WikiIRCBotListenerFactory listenerFactory;

    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event> asList(
            new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public String getName()
    {
        return "ircbot";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentModelBridge document = (DocumentModelBridge) source;
        DocumentReference documentReference = document.getDocumentReference();

        if (event instanceof DocumentCreatedEvent || event instanceof DocumentUpdatedEvent) {
            registerWikiListener(documentReference);
        } else if (event instanceof DocumentDeletedEvent) {
            unregisterWikiListener(documentReference);
        }
    }

    /**
     * @param documentReference the reference of the document containing the wiki Bot listener to register
     */
    private void registerWikiListener(DocumentReference documentReference)
    {
        // Unregister any existing listener registered under this document.
        unregisterWikiListener(documentReference);

        // Check whether the given document has a listener defined in it.
        if (this.listenerFactory.containsWikiListener(documentReference)) {
            // Attempt to create a wiki listener.
            WikiIRCBotListener wikiListener;
            try {
                wikiListener = this.listenerFactory.createWikiListener(documentReference);
            } catch (IRCBotException e) {
                this.logger.debug(String.format("Failed to create Wiki Bot Listener [%s]", documentReference), e);
                return;
            }

            // Register the listener.
            registerWikiListener(documentReference, wikiListener);
        }
    }

    /**
     * Register a new wiki Bot listener.
     *
     * @param documentReference the reference of the document containing the wiki listener to register
     * @param wikiListener the wiki listener to register
     */
    private void registerWikiListener(DocumentReference documentReference, WikiIRCBotListener wikiListener)
    {
        try {
            DefaultComponentDescriptor<IRCBotListener> componentDescriptor =
                new DefaultComponentDescriptor<IRCBotListener>();
            componentDescriptor.setRole(IRCBotListener.class);
            componentDescriptor.setRoleHint(this.entityReferenceSerializer.serialize(documentReference));
            this.componentManager.registerComponent(componentDescriptor, wikiListener);
        } catch (ComponentRepositoryException e) {
            this.logger.debug(
                String.format("Unable to register Wiki IRC Bot Listener in document [%s]", documentReference));
        }
    }


    /**
     * Unregister a wiki Bot listener.
     *
     * @param documentReference the reference of the document containing the wiki listener to unregister
     */
    private void unregisterWikiListener(DocumentReference documentReference)
    {
        String hint = this.entityReferenceSerializer.serialize(documentReference);
        this.componentManager.unregisterComponent(IRCBotListener.class, hint);
    }
}
