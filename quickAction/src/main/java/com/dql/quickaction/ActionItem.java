package com.dql.quickaction;

import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;

/**
 * We firstly must create an entity which will represent a particular item of out quick action. 
 * It will mainly contain an icon and a title (what is visible for the user). Besides this, the item
 * will have an id (in order to recognize it when we want to listen on click events on it), and 
 * two states sticky or not (i.e. after taping on the item the quick action will disappear or not).
 * It's basically an object that holds the information for a particular item from the quick action.
 * 
 * @author Phat (Phillip) H. VU <vuhongphat@hotmail.com>
 *
 */
public class ActionItem {
	private Drawable icon;
	private Bitmap thumb;
	private String title;
	private boolean selected;

	public ActionItem() {
	}

	public ActionItem(Drawable icon) {
		this.icon = icon;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return this.title;
	}

	public void setIcon(Drawable icon) {
		this.icon = icon;
	}

	public Drawable getIcon() {
		return this.icon;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return this.selected;
	}

	public void setThumb(Bitmap thumb) {
		this.thumb = thumb;
	}

	public Bitmap getThumb() {
		return this.thumb;
	}
}