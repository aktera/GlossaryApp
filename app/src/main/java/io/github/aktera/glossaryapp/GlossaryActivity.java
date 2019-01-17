package io.github.aktera.glossaryapp;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GlossaryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    public static final String INTENT_INDEX = "io.github.aktera.INTENT_INDEX";
    public static final String INTENT_GLOSSARY = "io.github.aktera.INTENT_GLOSSARY";
    public static final String INTENT_DESCRIPTION = "io.github.aktera.INTENT_DESCRIPTION";
    public static final String INTENT_TITLE = "io.github.aktera.INTENT_TITLE";
    public static final String SQL_LIST_GLOSSARY = "select * from Glossary order by glossary;";

    private static GlossaryOpenHelper helper;
    private static SQLiteDatabase db;
    private static ArrayList<Map<String, String>> arrayListItem;

    private final int REQUEST_IMPORT = 1000;
    private final int REQUEST_EXPORT = 1001;

    // アクティビティ作成時に呼ばれる
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 親クラスに処理を渡す
        super.onCreate(savedInstanceState);

        // レイアウトリソースを関連付け、部品をアクティビティに配置する
        setContentView(R.layout.activity_glossary);

        // ツールバーをアクションバーとして扱う
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ツールバーにアイコンを表示する
        //getSupportActionBar().setLogo(android.R.drawable.sym_def_app_icon);

        // データベースを開く
        helper = new GlossaryOpenHelper(getApplicationContext());
        db = helper.getWritableDatabase();

        // クエリを実行し、リスト項目を取得する
        arrayListItem = new ArrayList<Map<String, String>>();
        buildArrayListItem();

        // リストビューにデータを設定する
        String[] from = {"glossary", "_id"};
        int[] to = {R.id.textGlossary, R.id.textId};
        SimpleAdapter adapter = new SimpleAdapter(this, arrayListItem, R.layout.glossary_list_layout, from, to);
        ListView listGlossary = (ListView) findViewById(R.id.listGlossary);
        listGlossary.setAdapter(adapter);

        // リストビューにイベントを設定する
        listGlossary.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListView listView = (ListView) parent;
                Map<String, String> item = (Map<String, String>) listView.getItemAtPosition(position);
                openActivity(Integer.parseInt((String) item.get("_id")));
            }
        });

        // FABイベントを設定する
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivity(-1);
            }
        });

        // ドロワーが自動開閉できるようにする
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // ドロワーのイベントをこのアクティビティで受けるようにする
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    // 詳細ビューから処理が戻ってきたときに呼ばれる
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if ((requestCode == 1000) && (resultCode == Activity.RESULT_OK)) {
            ListView listGlossary = (ListView) findViewById(R.id.listGlossary);

            // Intentからデータを受け取る
            int index = intent.getIntExtra(INTENT_INDEX, -1);
            String glossary = intent.getStringExtra(INTENT_GLOSSARY);
            String description = intent.getStringExtra(INTENT_DESCRIPTION);

            ContentValues cv = new ContentValues();
            // 新規作成
            if ((index == -1) && (glossary.isEmpty() != true)) {
                // SQLiteのinsertの戻り値は追加されたレコードの _id フィールド値
                index = insertRecord(glossary, description);

                /* このコメント内のような実装をしたかったが、原因不明のエラーが発生。
                 * 新規登録→削除でなぜか落ちる。

                Map item = new HashMap();
                item.put("glossary", glossary);
                item.put("_id", index);
                arrayListItem.add(item);
                */

                /* 仕方ないので、下のような実装をとりあえず入れている。
                 * 全リスト項目を再構築するという、処理が非常に重いやつ。
                 */
                // リストビューを全更新する
                updateListView();
            }
            // 更新
            else if ((index != -1) && (glossary.isEmpty() != true)) {
                updateRecord(index, glossary, description);

                for (int i = 0; i < arrayListItem.size(); i++) {
                    Map<String, String> item = arrayListItem.get(i);
                    if (Integer.parseInt((String) item.get("_id")) == index) {
                        item.put("glossary", glossary);
                        break;
                    }
                }
                listGlossary.deferNotifyDataSetChanged();
                listGlossary.invalidateViews();
            }
            // 削除
            else if ((index != -1) && (glossary.isEmpty() == true)) {
                deleteRecord(index);

                for (int i = 0; i < arrayListItem.size(); i++) {
                    Map<String, String> item = arrayListItem.get(i);
                    if (Integer.parseInt((String) item.get("_id")) == index) {
                        arrayListItem.remove(i);
                        break;
                    }
                }
                listGlossary.deferNotifyDataSetChanged();
                listGlossary.invalidateViews();
            }
       }
    }

    // リストビューを全更新する
    private void updateListView() {
        arrayListItem.clear();

        // クエリを実行し、リスト項目を取得する
        buildArrayListItem();

        // リストビューを更新する
        ListView listGlossary = (ListView) findViewById(R.id.listGlossary);
        listGlossary.deferNotifyDataSetChanged();
        listGlossary.invalidateViews();
    }

    // 詳細ビューを開く
    private void openActivity(int index) {
        Intent intent = new Intent(getApplication(), DescriptionActivity.class);

        // 新規作成
        if (index == -1) {
            intent.putExtra(INTENT_INDEX, -1);
            intent.putExtra(INTENT_GLOSSARY, getString(R.string.new_glossary));
            intent.putExtra(INTENT_DESCRIPTION, getString(R.string.new_description));
            intent.putExtra(INTENT_TITLE, getString(R.string.title_new));
        }
        // リストアイテムが選択された
        else {
            Cursor cursor = db.rawQuery("select glossary,description from Glossary where _id=" + index + ";", null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String glossary = cursor.getString(cursor.getColumnIndex("glossary"));
                    String description = cursor.getString(cursor.getColumnIndex("description"));

                    intent.putExtra(INTENT_INDEX, index);
                    intent.putExtra(INTENT_GLOSSARY, glossary);
                    intent.putExtra(INTENT_DESCRIPTION, description);
                    intent.putExtra(INTENT_TITLE, getString(R.string.title_edit));
                }
                cursor.close();
            }
        }

        startActivityForResult(intent, 1000);
    }

    // ドロワーが開いているときに戻るボタンが押されたら呼ばれる
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ドロワーのメニューが選択されたときに呼ばれる
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.menu_import) {
            checkPermission(REQUEST_IMPORT);
        } else if (id == R.id.menu_export) {
            checkPermission(REQUEST_EXPORT);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // パーミッションを確認する
    public void checkPermission(int requestCode) {
        // パーミッションが取得済みか調べる
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // パーミッションを取得済み
            if (requestCode == REQUEST_IMPORT) {
                importDatabase();
            }
            else if (requestCode == REQUEST_EXPORT){
                exportDatabase();
            }
        }
        else {
            // 許可確認ダイアログを表示する必要があるか調べる
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showMessage(getString(R.string.external_storage_permission_denied));
            } else {
                // 許可確認ダイアログを表示する
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
            }
        }
    }

    // パーミッション許可確認ダイアログから呼ばれる
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_IMPORT: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 許可された
                    importDatabase();
                }
                return;
            }
            case REQUEST_EXPORT: {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 許可された
                    exportDatabase();
                }
                return;
            }
        }
    }

    // インポート
    private void importDatabase() {
        String externalPath = Environment.getExternalStorageDirectory().getPath() + "/Download/GlossaryApp.txt";
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;

        try {
            fis = new FileInputStream(externalPath);
            isr = new InputStreamReader(fis, "utf8");
            br = new BufferedReader(isr);

            // 開かれているデータベースを空にする
            helper.reset(db);

            while (true) {
                // パーシング：用語
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                String glossary = line;

                // パーシング：行数
                line = br.readLine();
                if (line == null) {
                    break;
                }
                int count = Integer.parseInt(line);

                // パーシング：詳細
                String description = null;
                for (int i = 0; i < count; i++) {
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    if (description == null) {
                        description = line;
                    }
                    else {
                        description = description + "\n" + line;
                    }
                }

                // データベースにレコードを追加する
                insertRecord(glossary, description);
            }

            br.close();

            // リストビューを全更新する
            updateListView();

            showMessage("インポートに成功しました");
        }
        catch (FileNotFoundException e) {
            showMessage(e.toString());
        }
        catch (UnsupportedEncodingException e) {
            showMessage(e.toString());
        }
        catch (IOException e) {
            showMessage(e.toString());
        }
    }

    // エクスポート
    private void exportDatabase() {
        String externalPath = Environment.getExternalStorageDirectory().getPath() + "/Download/GlossaryApp.txt";
        FileOutputStream fos = null;
        OutputStreamWriter osw = null;
        BufferedWriter bw = null;

        try {
            fos = new FileOutputStream(externalPath);
            osw = new OutputStreamWriter(fos, "utf8");
            bw = new BufferedWriter(osw);

            // 用語一覧を取得する
            Cursor cursor = db.rawQuery(SQL_LIST_GLOSSARY, null);
            for (boolean dataExist = cursor.moveToFirst(); dataExist; dataExist = cursor.moveToNext()) {
                bw.write(cursor.getString(cursor.getColumnIndex("glossary")) + "\n");
                String description = cursor.getString(cursor.getColumnIndex("description"));
                bw.write((countChar(description, '\n') + 1) + "\n");
                bw.write(description + "\n");
            }
            cursor.close();

            bw.flush();
            bw.close();

            showMessage("エクスポートに成功しました");
        }
        catch (IOException e) {
            showMessage(e.toString());
        }
    }

    // 文字列中にある文字の数をカウントする
    private int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    // クエリを実行し、リスト項目を取得する
    private void buildArrayListItem() {
        Cursor cursor = db.rawQuery(SQL_LIST_GLOSSARY, null);
        for (boolean dataExist = cursor.moveToFirst(); dataExist; dataExist = cursor.moveToNext()) {
            Map item = new HashMap();
            item.put("glossary", cursor.getString(cursor.getColumnIndex("glossary")));
            item.put("_id", String.valueOf(cursor.getInt(cursor.getColumnIndex("_id"))));
            arrayListItem.add(item);
        }
        cursor.close();
    }

    // レコードを追加する
    private int insertRecord(String glossary, String description) {
        ContentValues cv = new ContentValues();
        cv.put("glossary", glossary);
        cv.put("description", description);
        return (int)db.insert("Glossary", null, cv);
    }

    // レコードを更新する
    private void updateRecord(int id, String glossary, String description) {
        ContentValues cv = new ContentValues();
        cv.put("glossary", glossary);
        cv.put("description", description);
        db.update("Glossary", cv, "_id=" + id, null);
    }

    // レコードを削除する
    private void deleteRecord(int id) {
        db.delete("Glossary", "_id=" + id, null);
    }

    // 画面下部にメッセージをポップアップする
    private void showMessage(String message) {
        Snackbar.make(this.getWindow().getDecorView(), message, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
}