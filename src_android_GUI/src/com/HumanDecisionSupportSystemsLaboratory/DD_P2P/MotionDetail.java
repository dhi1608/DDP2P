/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import util.DD_Address;
import util.DD_SK;
import util.Util;
import config.Identity;
import data.D_Constituent;
import data.D_Document;
import data.D_Document_Title;
import data.D_Justification.JustificationSupportEntry;
import data.D_Motion;
import data.D_Motion.MotionChoiceSupport;
import data.D_MotionChoice;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

public class MotionDetail extends FragmentActivity {

	private WebView enhancingView;
	private String title;
	private String body;
	private String e_title;
	private String lid;
	// private String[] review;
	private Button choice_0;
	private Button choice_1;
	private Button choice_2;
	private ToAddJustificationDialog dialog;
	// private D_Justification justification;
	public static D_Motion crt_motion;
	protected static MotionDetail obj;
	private static final String BUNDLE_SAVED = "bundle_saved";

	private static final String TAG = "motion_detail";
	private static final boolean DEBUG = false;

	private Bundle b;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.motion_detail);

		obj = this;

		Intent intent = this.getIntent();
		b = intent.getExtras();

		title = b.getString(Motion.M_MOTION_TITLE);
		lid = b.getString(Motion.M_MOTION_LID);

		body = b.getString(Motion.M_MOTION_BODY);

		// review = new String[] { "Right!", "Good!" };

		// Log.d(TAG, savedInstanceState.toString());

		choice_0 = (Button) findViewById(R.id.motion_support);
		choice_1 = (Button) findViewById(R.id.motion_against);
		choice_2 = (Button) findViewById(R.id.motion_neutral);

		ViewPager pager = (ViewPager) findViewById(R.id.motion_detail_viewpager);
		if (DEBUG)
			Log.d("motion_title", "create activ");
		if (DEBUG)
			Log.d("motion_title", "viewpager created!");
		if (DEBUG)
			Log.d("motion_title", title);

		crt_motion = D_Motion.getMotiByLID(lid, true, false);

		if (crt_motion != null) {
			body = crt_motion.getMotionText().getDocumentUTFString();

			D_Motion enhanced = crt_motion.getEnhancedMotion();

			// String bdy = body;
			if (enhanced != null) {
				Object obj = enhanced.getTitleOrMy();
				e_title = null;
				if (obj instanceof D_Document)
					e_title = ((D_Document) obj).getDocumentUTFString();
				if (obj instanceof D_Document_Title)
					e_title = ((D_Document_Title) obj).title_document
							.getDocumentUTFString();
				// if (obj instanceof String) e_title = (String)obj;
				if (obj instanceof String)
					e_title = obj.toString();

				// bdy = //body + "\n\nEnhanced: " + "Enhancing: " + e_title;
				// enhancingView.setText("Enhancing: " + e_title);
/*				enhancingView.loadData("<B>Enhancing:</B> <i>" + e_title
						+ "</i>", "text/html", null);*/
			}
			// body =
			// justification.getJustificationText().getDocumentUTFString();
			if (DEBUG)
				Log.d("motion_body", body);
			// contentTextView.setText(body);
			// contentTextView.loadData(body, "text/html", null);

			D_MotionChoice[] choices = crt_motion.getActualChoices();
			if (choices.length >= 1) {
				MotionChoiceSupport l = crt_motion.getMotionSupport_WithCache(
						0, false);
				choice_0.setText(choices[0].name + " (" + l.getCnt() + ")");
				choice_0.setVisibility(android.view.View.VISIBLE);
			} else {
				choice_0.setVisibility(android.view.View.INVISIBLE);
			}
			if (choices.length >= 2) {
				MotionChoiceSupport l = crt_motion.getMotionSupport_WithCache(
						1, false);
				choice_1.setText(choices[1].name + " (" + l.getCnt() + ")");
				choice_1.setVisibility(android.view.View.VISIBLE);
			} else {
				choice_1.setVisibility(android.view.View.INVISIBLE);
			}
			if (choices.length >= 3) {
				MotionChoiceSupport l = crt_motion.getMotionSupport_WithCache(
						2, false);
				// Log.d("Vote", "l="+l.getCnt()+" w="+l.getWeight());
				choice_2.setText(choices[2].name + " (" + l.getCnt() + ")");
				choice_2.setVisibility(android.view.View.VISIBLE);
			} else {
				choice_2.setVisibility(android.view.View.INVISIBLE);
			}
		}

		pager.setAdapter(new MotionDetailAdapter(getSupportFragmentManager()));

		choice_0.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				long oLID;
				D_Constituent myself = Identity
						.getCrtConstituent(oLID = crt_motion
								.getOrganizationLID());
				if (myself == null) {
					if (DEBUG)
						Log.d("CONST", "MotionDetail ToAddJust: oLID=" + oLID
								+ " c=" + myself);
					Toast.makeText(MotionDetail.this, "Fill your Profile!",
							Toast.LENGTH_LONG).show();
					return;
				}
				// D_Justification justification = null;
				// if (JustificationBySupportType.checkedJustifLID > 0)
				// justification =
				// D_Justification.getJustByLID(JustificationBySupportType.checkedJustifLID,
				// true, false);
				JustificationSupportEntry justification = null;
				if (JustificationBySupportType.checkedJustifLID > 0) {
					justification = new JustificationSupportEntry();
					justification.setJustification_LID(Util
							.getStringID(JustificationBySupportType.checkedJustifLID));
				}
				// update name dialog
				FragmentManager fm = getSupportFragmentManager();
				dialog = new ToAddJustificationDialog(crt_motion,
						justification, MotionDetail.crt_motion
								.getActualChoices()[0].short_name);
				dialog.show(fm, "fragment_to_add_justification");

			}
		});

		choice_1.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				long oLID;
				D_Constituent myself = Identity
						.getCrtConstituent(oLID = crt_motion
								.getOrganizationLID());
				if (myself == null) {
					if (DEBUG)
						Log.d("CONST", "MotionDetail ToAddJust: oLID=" + oLID
								+ " c=" + myself);
					Toast.makeText(MotionDetail.this, "Fill your Profile!",
							Toast.LENGTH_LONG).show();
					return;
				}
				JustificationSupportEntry justification = null;
				if (JustificationBySupportType.checkedJustifLID > 0) {
					justification = new JustificationSupportEntry();
					justification.setJustification_LID(Util
							.getStringID(JustificationBySupportType.checkedJustifLID));
				}

				FragmentManager fm = getSupportFragmentManager();
				dialog = new ToAddJustificationDialog(crt_motion,
						justification, MotionDetail.crt_motion
								.getActualChoices()[1].short_name);
				dialog.show(fm, "fragment_to_add_justification");
			}
		});

		choice_2.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				long oLID;
				D_Constituent myself = Identity
						.getCrtConstituent(oLID = crt_motion
								.getOrganizationLID());
				if (myself == null) {
					if (DEBUG)
						Log.d("CONST", "MotionDetail ToAddJust: oLID=" + oLID
								+ " c=" + myself);
					Toast.makeText(MotionDetail.this, "Fill your Profile!",
							Toast.LENGTH_LONG).show();
					return;
				}
				JustificationSupportEntry justification = null;
				if (JustificationBySupportType.checkedJustifLID > 0) {
					justification = new JustificationSupportEntry();
					justification.setJustification_LID(Util
							.getStringID(JustificationBySupportType.checkedJustifLID));
				}
				FragmentManager fm = getSupportFragmentManager();
				dialog = new ToAddJustificationDialog(crt_motion,
						justification, MotionDetail.crt_motion
								.getActualChoices()[2].short_name);
				dialog.show(fm, "fragment_to_add_justification");
			}
		});

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(Motion.M_MOTION_TITLE, title);
		outState.putString(Motion.M_MOTION_LID, lid);
		outState.putString(Motion.M_MOTION_LID, body);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {

		super.onRestoreInstanceState(savedInstanceState);
		title = savedInstanceState.getString(Motion.M_MOTION_TITLE);
		lid = savedInstanceState.getString(Motion.M_MOTION_LID);
		body = savedInstanceState.getString(Motion.M_MOTION_LID);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.motion_detail_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.motion_detail_add_new_motion) {
			Toast.makeText(this, "add a new Motion", Toast.LENGTH_SHORT).show();

			Intent intent = new Intent();
			intent.setClass(this, AddMotion.class);

			Bundle b = new Bundle();
			b.putString(Motion.M_MOTION_LID, crt_motion.getLIDstr());
			intent.putExtras(b);

			startActivity(intent);
		}

		if (item.getItemId() == R.id.view_votes) {
			Intent intent = new Intent();
			intent.setClass(this, ViewVotes.class);

			Bundle b = new Bundle();
			b.putString(Motion.M_MOTION_LID, crt_motion.getLIDstr());
			if (JustificationBySupportType.checkedJustifLID > 0) {
				b.putString(
						Motion.J_JUSTIFICATION_LID,
						Util.getStringID(JustificationBySupportType.checkedJustifLID));
			} else {
				// put a nonull value to get only the votes with no
				// justification!
			}
			intent.putExtras(b);

			startActivity(intent);

		}

		if (item.getItemId() == R.id.motion_detail_export) {
			DD_SK d_SK = new DD_SK();
			DD_SK.addMotionToDSSK(d_SK, crt_motion);
			/*
			DD_SK_Entry dsk = new DD_SK_Entry();
			dsk.key = constituent.getSK();
			dsk.name = constituent.getNameOrMy();
			dsk.creation = constituent.getCreationDate();
			dsk.type = "Motion Detail!";
			d_SK.sk.add(dsk);
			*/
			
			String testText = Safe.getExportTextObject(d_SK.encode());
			String testSubject = "DDP2P: Motion Detail of \""+ crt_motion.getTitleStrOrMy() + "\" in \""+ crt_motion.getOrganization().getName()+"\" " + Safe.SAFE_TEXT_MY_HEADER_SEP;

			/*
			 * if (organization_gidh == null) { Toast.makeText(this,
			 * "No peer. Reload!", Toast.LENGTH_SHORT).show(); return true; }
			 */
			DD_Address adr = new DD_Address();

			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_TEXT, testText);
			i.putExtra(Intent.EXTRA_SUBJECT,
					testSubject);
			i = Intent.createChooser(i, "send motion Public key");
			startActivity(i);
		}

		return super.onOptionsItemSelected(item);
	}

	private class MotionDetailAdapter extends FragmentPagerAdapter {

		public MotionDetailAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);

		}

		@Override
		public Fragment getItem(int pos) {
			/*
			 * switch (pos) { case 0: return
			 * JustificationBySupportType.newInstance
			 * ("FirstFragment, Instance 1"); case 1: return
			 * JustificationsAll.newInstance("SecondFragment, Instance 1"); }
			 */
			Log.d(TAG, "MotionDetail: getPos " + pos + " ->title=" + title);
			if (pos == MotionDetail.crt_motion.getActualChoices().length)
				return JustificationBySupportType.newInstance(title, body,
						e_title, "FirstFragment, Instance", pos);
			// return
			// JustificationsAll.newInstance("SecondFragment, Instance 1");
			if (pos < MotionDetail.crt_motion.getActualChoices().length)
				return JustificationBySupportType.newInstance(title, body,
						e_title, "FirstFragment, Instance", pos);
			return null;
		}

		@Override
		public int getCount() {
			return MotionDetail.crt_motion.getActualChoices().length + 1;
		}

	}

}