package cc.metapro.openct.data.university;

/*
 *  Copyright 2016 - 2017 OpenCT open source class table
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Keep;

import cc.metapro.openct.R;
import cc.metapro.openct.custom.CustomConfiguration;
import cc.metapro.openct.data.source.StoreHelper;

@Keep
public class AdvancedCustomInfo {

    public CustomConfiguration mWebScriptConfiguration;
    public CmsFactory.ClassTableInfo mClassTableInfo;
    public String CLASS_URL_PATTERN;
    public String GRADE_URL_PATTERN;
    public String BORROW_URL_PATTERN;
    public String GRADE_TABLE_ID;
    public String BORROW_TABLE_ID;
    public String mCmsURL;
    private String mSchoolName;

    public AdvancedCustomInfo(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (preferences.getBoolean(context.getString(R.string.pref_custom_enable), false)) {
            mSchoolName = preferences.getString(context.getString(R.string.pref_school_name), "openct");
        } else {
            mSchoolName = preferences.getString(context.getString(R.string.pref_school_name), context.getResources().getStringArray(R.array.school_names)[0]);
        }
    }

    public String getSchoolName() {
        return mSchoolName;
    }

    public void setClassTableInfo(CmsFactory.ClassTableInfo classTableInfo) {
        mClassTableInfo = classTableInfo;
    }

    public void setWebScriptConfiguration(CustomConfiguration webScriptConfiguration) {
        mWebScriptConfiguration = webScriptConfiguration;
    }

    public void setCmsURL(String cmsURL) {
        mCmsURL = cmsURL;
    }

    @Override
    public String toString() {
        return StoreHelper.toJson(this);
    }
}