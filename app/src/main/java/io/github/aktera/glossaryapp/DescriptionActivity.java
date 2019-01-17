package io.github.aktera.glossaryapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class DescriptionActivity extends AppCompatActivity {
    private int index;
    private String glossary;
    private String description;
    private String title;

    // アクティビティ作成時に呼ばれる
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // レイアウトリソースを関連付け、部品をアクティビティに配置する
        setContentView(R.layout.activity_description);

        // ツールバーをアクションバーとして扱う
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ツールバーに戻るボタンを追加する
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Intentからインデックスを受け取る
        Intent intent = getIntent();
        index = intent.getIntExtra(GlossaryActivity.INTENT_INDEX, -1);
        glossary = intent.getStringExtra(GlossaryActivity.INTENT_GLOSSARY);
        description = intent.getStringExtra(GlossaryActivity.INTENT_DESCRIPTION);
        title = intent.getStringExtra(GlossaryActivity.INTENT_TITLE);

        // ウィジェットにデータを設定する
        EditText editGlossary = (EditText) findViewById(R.id.editGlossary);
        editGlossary.setText(glossary);
        EditText editDescription = (EditText) findViewById(R.id.editDescription);
        editDescription.setText(description);
        setTitle(title);

        // FABイベントを設定する
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editGlossary = (EditText) findViewById(R.id.editGlossary);
                glossary = editGlossary.getText().toString();
                EditText editDescription = (EditText) findViewById(R.id.editDescription);
                description = editDescription.getText().toString();
                returnIntent();
            }
        });
    }

    // メニュー表示時に呼ばれる
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // メニューリソースを関連付ける
        getMenuInflater().inflate(R.menu.menu_description, menu);
        return true;
    }

    // メニューアイテム選択時に呼ばれる
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            // 削除メニュー
            case R.id.menu_delete:
                glossary = "";
                description = "";
                returnIntent();
                return true;
            // 戻るメニュー
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // 親アクティビティにIntentを渡して閉じる
    private void returnIntent() {
        Intent intent = new Intent();
        intent.putExtra(GlossaryActivity.INTENT_INDEX, index);
        intent.putExtra(GlossaryActivity.INTENT_GLOSSARY, glossary);
        intent.putExtra(GlossaryActivity.INTENT_DESCRIPTION, description);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}