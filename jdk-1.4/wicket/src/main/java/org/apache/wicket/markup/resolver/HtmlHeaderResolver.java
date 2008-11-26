/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.markup.resolver;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupException;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.WicketTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.parser.filter.HtmlHeaderSectionHandler;
import org.apache.wicket.markup.parser.filter.WicketTagIdentifier;

/**
 * This is a tag resolver which handles &lt;head&gt; and &lt;wicket:head&gt;tags. It must be
 * registered (with the application) and assumes that a ComponentTag respectively a WicketTag has
 * already been created (see HtmlheaderSectionHandler and WicketTagIdentifier).
 * <p>
 * Provided the current tag is a &lt;head&gt;, a HtmlHeaderContainer component is created, (auto)
 * added to the component hierarchie and immediately rendered. Please see the javadoc for
 * HtmlHeaderContainer on how it treats the tag.
 * <p>
 * In case of &lt;wicket:head&gt; a simple WebMarkupContainer handles the tag.
 * 
 * @author Juergen Donnerstag
 */
public class HtmlHeaderResolver implements IComponentResolver
{
	private static final long serialVersionUID = 1L;

	static
	{
		// register "wicket:head"
		WicketTagIdentifier.registerWellKnownTagName("head");
	}

	/**
	 * Try to resolve the tag, then create a component, add it to the container and render it.
	 * 
	 * @see org.apache.wicket.markup.resolver.IComponentResolver#resolve(MarkupContainer,
	 *      MarkupStream, ComponentTag)
	 * 
	 * @param container
	 *            The container parsing its markup
	 * @param markupStream
	 *            The current markupStream
	 * @param tag
	 *            The current component tag while parsing the markup
	 * @return true, if componentId was handle by the resolver. False, otherwise
	 */
	public boolean resolve(final MarkupContainer container, final MarkupStream markupStream,
		final ComponentTag tag)
	{
		// Only <head> component tags have the id == "_header"
		if (tag.getId().equals(HtmlHeaderSectionHandler.HEADER_ID))
		{
			// Create a special header component which will gather additional
			// input the <head> from 'contributors'.
			final WebMarkupContainer header = new HtmlHeaderContainer(
				HtmlHeaderSectionHandler.HEADER_ID + container.getPage().getAutoIndex());
			container.autoAdd(header, markupStream);

			// Yes, we handled the tag
			return true;
		}
		else if ((tag instanceof WicketTag) && ((WicketTag)tag).isHeadTag())
		{
			// If we found <wicket:head> without surrounding <head> on a Page,
			// than we have to add wicket:head into a automatically generated
			// head first.
			if (container instanceof WebPage)
			{
				// Create a special header component which will gather
				// additional input the <head> from 'contributors'.
				final MarkupContainer header = new HtmlHeaderContainer(
					HtmlHeaderSectionHandler.HEADER_ID + container.getPage().getAutoIndex());

				// It is <wicket:head>. Because they do not provide any
				// additional functionality they are merely a means of surrounding relevant
				// markup. Thus we simply create a WebMarkupContainer to handle
				// the tag.
				final WebMarkupContainer header2 = new WebMarkupContainer(
					HtmlHeaderSectionHandler.HEADER_ID)
				{
					private static final long serialVersionUID = 1L;

					public boolean isTransparentResolver()
					{
						return true;
					}
				};

				header2.setRenderBodyOnly(true);

				header.add(header2);

				container.autoAdd(header, markupStream);
			}
			else if (container instanceof HtmlHeaderContainer)
			{
				// It is <wicket:head>. Because they do not provide any
				// additional functionality there are merely a means of surrounding
				// relevant markup. Thus we simply create a WebMarkupContainer to handle
				// the tag.
				final WebMarkupContainer header = new WebMarkupContainer(
					HtmlHeaderSectionHandler.HEADER_ID)
				{
					private static final long serialVersionUID = 1L;

					public boolean isTransparentResolver()
					{
						return true;
					}
				};
				header.setRenderBodyOnly(true);

				try
				{
					container.autoAdd(header, markupStream);
				}
				catch (IllegalArgumentException ex)
				{
					throw new WicketRuntimeException("If the root exception says something like "
						+ "\"A child with id '_header' already exists\" "
						+ "then you most likely forgot to override autoAdd() "
						+ "in your bordered page component.", ex);
				}
			}
			else
			{
				throw new MarkupException(
					"Mis-placed <wicket:head>. <wicket:head> must be outside of <wicket:panel>, <wicket:border>, and <wicket:extend>");
			}

			// Yes, we handled the tag
			return true;
		}

		// We were not able to handle the tag
		return false;
	}
}