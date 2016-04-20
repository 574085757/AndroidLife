/*
 * Copyright (C) 2016 CaMnter yuanyu.camnter@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.camnter.newlife.views.activity.easyrecyclerviewsidebar;

import com.camnter.newlife.adapter.easyrecyclerviewsidebar.LetterSectionAdapter;
import com.camnter.newlife.bean.Contacts;
import com.camnter.newlife.constant.Constant;
import java.util.List;

/**
 * Description：LetterSectionActivity
 * Created by：CaMnter
 * Time：2016-04-12 22:42
 */
public class LetterSectionActivity extends SectionActivity {

    @Override public void setActivityTitle() {
        this.setTitle("LetterSectionActivity");
    }


    @Override public void initAdapter() {
        this.adapter = new LetterSectionAdapter();
    }


    @Override public List<Contacts> getData() {
        return Constant.letterSectionList;
    }
}
