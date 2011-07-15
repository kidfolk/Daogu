package com.kidfolk.daogu;

import java.util.ArrayList;
import java.util.List;

import weibo4android.Paging;
import weibo4android.Status;
import weibo4android.Weibo;
import weibo4android.WeiboException;
import weibo4android.http.AccessToken;
import weibo4android.http.RequestToken;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WeiboListActivity extends ListActivity {
	private Weibo weibo;
	public static final int TWEET_REQUEST = 0;
	public static final int RETWEET_OK = 1;
	public static final int RETWEET_FAILURE = 2;
	public static final int GETWEIBOLIST_OK = 3;
	public static final int REPLY_REQUEST = 4;
	// public static final int RESULT_FAUILE = 1;

	private ImageButton tweet;
	private ImageButton refresh;
	private Handler handler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// 根据id获得view对象
		tweet = (ImageButton) findViewById(R.id.tweet);
		refresh = (ImageButton) findViewById(R.id.refresh);
		// 创建监听器内部类对象
		ImageButtonListener listener = new ImageButtonListener();
		// 给ImageButton添加事件
		tweet.setOnClickListener(listener);
		refresh.setOnClickListener(listener);

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				//处理获取主页微博消息
				case GETWEIBOLIST_OK:
					DaoguAdapter adapter = new DaoguAdapter(
							WeiboListActivity.this.getApplicationContext(),
							(List<Status>) msg.obj);
					WeiboListActivity.this.setListAdapter(adapter);
					break;
					//处理转发成功消息
				case RETWEET_OK:
					Toast.makeText(WeiboListActivity.this, "转发成功！", Toast.LENGTH_SHORT).show();
					break;
					//处理转发失败消息
				case RETWEET_FAILURE:
					Toast.makeText(WeiboListActivity.this, "转发失败！", Toast.LENGTH_SHORT).show();
					break;
				}
			}
		};
		weibo = OAuthConstant.getInstance().getWeibo();
		registerForContextMenu(this.findViewById(android.R.id.list));
		// 初始化
		init();
		/* 显示我的微博和关注人的微博 */

		getMyHomeTimeLine();
	}

	private void init() {
		new Thread(new Runnable() {

			@Override
			public void run() {

				Uri uri = WeiboListActivity.this.getIntent().getData();
				String oauth_verifier = uri.getQueryParameter("oauth_verifier");
				if (null == oauth_verifier) {
					// 已经授权
					weibo.setOAuthAccessToken(OAuthConstant.getInstance()
							.getAccessToken());
				} else {
					/* 保存授权信息 */

					try {
						RequestToken requestToken = OAuthConstant.getInstance()
								.getRequestToken();
						AccessToken accessToken = requestToken
								.getAccessToken(oauth_verifier);// 获得授权码
						OAuthConstant.getInstance().setAccessToken(accessToken);
						OAuthConstant.getInstance().setTokenSecret(
								accessToken.getTokenSecret());
						User user = new User();
						user.setId(String.valueOf(accessToken.getUserId()));
						user.setAccesstoken(accessToken.getToken());
						user.setAccesstoken_secret(accessToken.getTokenSecret());
						List<User> userList = new ArrayList<User>();
						userList.add(user);
						XMLReaderAndWriter<User> writer = new UserReaderAndWriter(
								getApplicationContext());
						writer.writer(userList);
					} catch (WeiboException e) {
						e.printStackTrace();
					}
				}

			}

		}).start();

	}

	private void getMyHomeTimeLine() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					List<Status> statusList = weibo.getHomeTimeline();
					if (null != statusList & statusList.size() > 0) {
						// 取得weibo数据
						Message msg = new Message();
						msg.obj = statusList;
						msg.what = GETWEIBOLIST_OK;
						WeiboListActivity.this.handler.sendMessage(msg);
					}
				} catch (WeiboException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).start();

	}

	/**
	 * ImageButton的监听器
	 * 
	 * @author kidfolk
	 * 
	 */
	class ImageButtonListener implements View.OnClickListener {

		@Override
		public void onClick(View v) {
			int id = v.getId();
			switch (id) {
			case R.id.tweet:
				Intent intent = new Intent("com.kidfolk.daogu.TWEET",
						Uri.parse("daogu://TweetWeiboActivity"));
				startActivityForResult(intent, TWEET_REQUEST);
				break;
			case R.id.refresh:
				getMyHomeTimeLine();
				break;
			}

		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == TWEET_REQUEST && resultCode == RESULT_OK) {
			Toast.makeText(WeiboListActivity.this, "发送成功！", Toast.LENGTH_SHORT)
					.show();
		}
	}

	// @Override
	// protected void onListItemClick(ListView l, View v, int position, long id)
	// {
	// Status status = (Status) l.getItemAtPosition(position);
	// status.getId();
	// }

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		int id = item.getItemId();
		//获得选中的微博
		final Status status = (Status) this.getListView()
		.getItemAtPosition(info.position);
		switch (id) {
		case R.id.retweet:
			
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						// 转发
						Status result = weibo.retweetStatus(status.getId());
						Message msg = new Message();
						if (null != result) {
							msg.obj = result;
							msg.what = RETWEET_OK;
						} else {
							msg.what = RETWEET_FAILURE;
						}
						WeiboListActivity.this.handler.sendMessage(msg);
					} catch (WeiboException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}).start();

			return true;
		case R.id.reply:
			Intent intent = new Intent("com.kidfolk.daogu.REPLY",Uri.parse("daogu://ReplyWeiboActivity"));
			intent.putExtra("status", status);
			startActivityForResult(intent, REPLY_REQUEST);
			return true;
		default:
			return super.onContextItemSelected(item);

		}

	}

	/**
	 * 创建contextmenu
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.weibo_context_menu, menu);
	}

}
