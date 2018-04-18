/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.sfsu.csc780.chathub.model;

public class ChatMessage {

    private String text;
    private String name;
    private String photoUrl;
    private String imageUrl;
    private boolean animateBackgroundHeart = false;

    public ChatMessage() {
    }

    public ChatMessage(String text, String name, String photoUrl, boolean animateBackgroundHeart) {
        this.text = text;
        this.name = name;
        this.photoUrl = photoUrl;
        this.animateBackgroundHeart = animateBackgroundHeart;
    }

    public ChatMessage(String text, String name, String photoUrl,
                       String imageUrl, boolean animateBackgroundHeart) {
        this(text, name, photoUrl, animateBackgroundHeart);
        this.imageUrl = imageUrl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getAnimateBackgroundHeart() {
        return animateBackgroundHeart;
    }

    public void setAnimateBackgroundHeart(Boolean animateBackgroundHeart) {
        this.animateBackgroundHeart = animateBackgroundHeart;
    }
}
