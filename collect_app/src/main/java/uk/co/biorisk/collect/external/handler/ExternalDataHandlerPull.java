/*
 * Copyright (C) 2014 University of Washington
 *
 * Originally developed by Dobility, Inc. (as part of SurveyCTO)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package uk.co.biorisk.collect.external.handler;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.expr.XPathFuncExpr;

import java.util.ArrayList;
import java.util.List;

import uk.co.biorisk.collect.external.ExternalDataManager;
import uk.co.biorisk.collect.external.ExternalDataUtil;
import uk.co.biorisk.collect.external.ExternalSQLiteOpenHelper;

/**
 * Author: Meletis Margaritis
 * Date: 25/04/13
 * Time: 13:50
 */
public class ExternalDataHandlerPull extends ExternalDataHandlerBase {

    public static final String HANDLER_NAME = "pulldata";

    public ExternalDataHandlerPull(ExternalDataManager externalDataManager) {
        super(externalDataManager);
    }

    @Override
    public String getName() {
        return HANDLER_NAME;
    }

    @Override
    public List<Class[]> getPrototypes() {
        return new ArrayList<>();
    }

    @Override
    public boolean rawArgs() {
        return true;
    }

    @Override
    public boolean realTime() {
        return false;
    }

    @Override
    public Object eval(Object[] args, EvaluationContext ec) {
        if (args.length != 4) {
            Log.e(ExternalDataUtil.LOGGER_NAME, "4 arguments are needed to evaluate the " + HANDLER_NAME + " function");
            return "";
        }

        String dataSetName = XPathFuncExpr.toString(args[0]);
        String queriedColumn = XPathFuncExpr.toString(args[1]);
        String referenceColumn = XPathFuncExpr.toString(args[2]);
        String referenceValue = XPathFuncExpr.toString(args[3]);

        // SCTO-545
        dataSetName = normalize(dataSetName);

        ExternalSQLiteOpenHelper sqLiteOpenHelper = getExternalDataManager().getDatabase(dataSetName, false);
        String[] selectionArgs = {referenceValue};
        String selection = ExternalDataUtil.toSafeColumnName(referenceColumn) + "=?";
        String[] columns = {ExternalDataUtil.toSafeColumnName(queriedColumn)};
        SQLiteDatabase db = sqLiteOpenHelper.getReadableDatabase();
        try (Cursor c = db.query(ExternalDataUtil.EXTERNAL_DATA_TABLE_NAME, columns, selection, selectionArgs, null, null, null)) {

            if (c.getCount() > 0) {
                c.moveToFirst();
                return ExternalDataUtil.nullSafe(c.getString(0));
            } else {
                Log.e(ExternalDataUtil.LOGGER_NAME, "Could not find a value in " + queriedColumn + " where the column " + referenceColumn + " has the value " + referenceValue);
                return "";
            }
        } catch (Exception e) {
            Log.e(ExternalDataUtil.LOGGER_NAME, e.getMessage());
            return "";
        }
    }
}
