/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 21:01:15
 */
package com.xpn.xwiki.render;

import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.XWikiContext;

public class XWikiPluginRenderer implements XWikiRenderer {
    public XWikiPluginRenderer() {
    }

    public String render(String content, XWikiDocInterface doc, XWikiContext context) {
        return content;
    }
}
