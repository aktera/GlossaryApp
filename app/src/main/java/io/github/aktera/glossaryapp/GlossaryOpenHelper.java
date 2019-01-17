package io.github.aktera.glossaryapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GlossaryOpenHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "io.github.aktera.glossaryapp.db";
    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE Glossary (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, glossary TEXT, description TEXT);";
    public static final String SQL_DROP_TABLE =
            "DROP TABLE IF EXISTS Glossary;";

    // コンストラクタ
    public GlossaryOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // テーブル作成時に呼ばれる
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    // データベースのバージョンが低い時に呼ばれる
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }

    // データベースのバージョンが高い時に呼ばれる
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    // データを初期化する
    public void reset(SQLiteDatabase db) {
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }
}