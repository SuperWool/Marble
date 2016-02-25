package sim.marble;

import sim.example.readandroid.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

/**
 * This is the splash screen that the user sees at the start of the app.
 */
public class IntroSplash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_intro_splash);
		ImageView intro = (ImageView)findViewById(R.id.intro);
		ScaleAnimation introAnim = (ScaleAnimation)AnimationUtils.loadAnimation
				(this, R.anim.intro_anim);
		// Uncomment these two lines and outcomment the line below to have a delay
//		introAnim.setStartTime(0);
//		intro.startAnimation(introAnim);
		intro.setAnimation(introAnim);
		startMenu();
	}

	private void startMenu() {
		new Handler().postDelayed(new Runnable() {
			public void run() {
				Intent mainMenu = new Intent(IntroSplash.this, MainMenu.class);
				mainMenu.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(mainMenu);
			}
		}, 1500);
	}
}
