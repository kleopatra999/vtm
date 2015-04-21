/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.tiling.source;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.oscim.core.Tile;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.LwHttp.LwHttpFactory;

public abstract class UrlTileSource extends TileSource {

	public abstract static class Builder<T extends Builder<T>> extends TileSource.Builder<T> {
		protected int zoomMin, zoomMax;
		protected String tilePath;
		protected String url;
		private HttpEngine.Factory engineFactory;
		private String apiKey;

		protected Builder() {

		}

		protected Builder(String url, String tilePath, int zoomMin, int zoomMax) {
			this.url = url;
			this.tilePath = tilePath;
			this.zoomMin = zoomMin;
			this.zoomMax = zoomMax;
		}

		public T tilePath(String tilePath) {
			this.tilePath = tilePath;
			return self();
		}

		public T url(String url) {
			this.url = url;
			return self();
		}

		public T httpFactory(HttpEngine.Factory factory) {
			this.engineFactory = factory;
			return self();
		}

	}

	public final static TileUrlFormatter URL_FORMATTER = new DefaultTileUrlFormatter();
	private final URL mUrl;
	private final String[] mTilePath;

	private HttpEngine.Factory mHttpFactory;
	private Map<String, String> mRequestHeaders = Collections.emptyMap();
	private TileUrlFormatter mTileUrlFormatter = URL_FORMATTER;
	private String apiKey;

	public interface TileUrlFormatter {
		public String formatTilePath(UrlTileSource tileSource, Tile tile);
	}

	protected UrlTileSource(Builder<?> builder) {
		this(builder.url, builder.tilePath, builder.zoomMin, builder.zoomMax);
		mHttpFactory = builder.engineFactory;
	}

	protected UrlTileSource(String urlString, String tilePath) {
		this(urlString, tilePath, 0, 17);
	}

	protected UrlTileSource(String urlString, String tilePath, int zoomMin, int zoomMax) {
		super(zoomMin, zoomMax);
		if (tilePath == null)
			throw new IllegalArgumentException("tilePath cannot be null.");

		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
		mUrl = url;
		mTilePath = tilePath.split("\\{|\\}");
	}

	@Override
	public OpenResult open() {
		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {

	}

	public URL getUrl() {
		return mUrl;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getTileUrl(Tile tile) {
		String tileUrl = mUrl + mTileUrlFormatter.formatTilePath(this, tile);
		if (apiKey != null) {
			tileUrl += String.format("?api_key=%s", apiKey);
		}
		return tileUrl;
	}

	public void setHttpEngine(HttpEngine.Factory httpFactory) {
		mHttpFactory = httpFactory;
	}

	public void setHttpRequestHeaders(Map<String, String> options) {
		mRequestHeaders = options;
	}

	public Map<String, String> getRequestHeader() {
		return mRequestHeaders;
	}

	public String[] getTilePath() {
		return mTilePath;
	}

	/**
	 * 
	 */
	public void setUrlFormatter(TileUrlFormatter formatter) {
		mTileUrlFormatter = formatter;
	}

	public TileUrlFormatter getUrlFormatter() {
		return mTileUrlFormatter;
	}

	public HttpEngine getHttpEngine() {
		if (mHttpFactory == null) {
			mHttpFactory = new LwHttpFactory();
		}
		return mHttpFactory.create(this);
	}

	static class DefaultTileUrlFormatter implements TileUrlFormatter {
		@Override
		public String formatTilePath(UrlTileSource tileSource, Tile tile) {

			StringBuilder sb = new StringBuilder();
			for (String b : tileSource.getTilePath()) {
				if (b.length() == 1) {
					switch (b.charAt(0)) {
						case 'X':
							sb.append(tile.tileX);
							continue;
						case 'Y':
							sb.append(tile.tileY);
							continue;
						case 'Z':
							sb.append(tile.zoomLevel);
							continue;
						default:
							break;
					}
				}
				sb.append(b);
			}
			return sb.toString();
		}
	}
}
