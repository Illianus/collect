/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.widgets;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.R;
import org.odk.collect.android.activities.FormEntryActivity;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.android.utilities.MediaUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Widget that allows user to take pictures, sounds or video and add them to the form.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class ImageWidget extends QuestionWidget implements IBinaryWidget {
    private final static String t = "MediaWidget";

    private static final int ORIENTATION_NORMAL=3;
    private static final int ORIENTATION_LANDSCAPE=1;
    private static final int ORIENTATION_INVERTED_LANDSCAPE=6;
    private static final int ORIENTATION_INVERTED=8;

    private Button mCaptureButton;
    private Button mChooseButton;
    private ImageView mImageView;

    private String mBinaryName;

    private String mInstanceFolder;
    
    private TextView mErrorTextView;


    public ImageWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        mInstanceFolder =
                Collect.getInstance().getFormController().getInstancePath().getParent();

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.setMargins(7, 5, 7, 5);
        
        mErrorTextView = new TextView(context);
        mErrorTextView.setId(QuestionWidget.newUniqueId());
        mErrorTextView.setText("Selected file is not a valid image");

        // setup capture button
        mCaptureButton = new Button(getContext());
        mCaptureButton.setId(QuestionWidget.newUniqueId());
        mCaptureButton.setText(getContext().getString(R.string.capture_image));
        mCaptureButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mCaptureButton.setPadding(20, 20, 20, 20);
        mCaptureButton.setEnabled(!prompt.isReadOnly());
        mCaptureButton.setLayoutParams(params);

        // launch capture intent on click
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               	Collect.getInstance().getActivityLogger().logInstanceAction(this, "captureButton", 
            			"click", mPrompt.getIndex());
                mErrorTextView.setVisibility(View.GONE);
                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                // We give the camera an absolute filename/path where to put the
                // picture because of bug:
                // http://code.google.com/p/android/issues/detail?id=1480
                // The bug appears to be fixed in Android 2.0+, but as of feb 2,
                // 2010, G1 phones only run 1.6. Without specifying the path the
                // images returned by the camera in 1.6 (and earlier) are ~1/4
                // the size. boo.

                // if this gets modified, the onActivityResult in
                // FormEntyActivity will also need to be updated.
                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(Collect.TMPFILE_PATH)));
                try {
                	Collect.getInstance().getFormController().setIndexWaitingForData(mPrompt.getIndex());
                    ((Activity) getContext()).startActivityForResult(i,
                        FormEntryActivity.IMAGE_CAPTURE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(),
                        getContext().getString(R.string.activity_not_found, "image capture"),
                        Toast.LENGTH_SHORT).show();
                	Collect.getInstance().getFormController().setIndexWaitingForData(null);
                }

            }
        });

        // setup chooser button
        mChooseButton = new Button(getContext());
        mChooseButton.setId(QuestionWidget.newUniqueId());
        mChooseButton.setText(getContext().getString(R.string.choose_image));
        mChooseButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);
        mChooseButton.setPadding(20, 20, 20, 20);
        mChooseButton.setEnabled(!prompt.isReadOnly());
        mChooseButton.setLayoutParams(params);

        // launch capture intent on click
        mChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               	Collect.getInstance().getActivityLogger().logInstanceAction(this, "chooseButton", 
            			"click", mPrompt.getIndex());
                mErrorTextView.setVisibility(View.GONE);
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");

                try {
					Collect.getInstance().getFormController()
							.setIndexWaitingForData(mPrompt.getIndex());
                    ((Activity) getContext()).startActivityForResult(i,
                        FormEntryActivity.IMAGE_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(),
                        getContext().getString(R.string.activity_not_found, "choose image"),
                        Toast.LENGTH_SHORT).show();
                	Collect.getInstance().getFormController().setIndexWaitingForData(null);
                }

            }
        });

        // finish complex layout
        LinearLayout answerLayout = new LinearLayout(getContext());
        answerLayout.setOrientation(LinearLayout.VERTICAL);
        answerLayout.addView(mCaptureButton);
        answerLayout.addView(mChooseButton);
        answerLayout.addView(mErrorTextView);

        // and hide the capture and choose button if read-only
        if ( prompt.isReadOnly() ) {
        	mCaptureButton.setVisibility(View.GONE);
        	mChooseButton.setVisibility(View.GONE);
        }
        mErrorTextView.setVisibility(View.GONE);

        // retrieve answer from data model and update ui
        mBinaryName = prompt.getAnswerText();

        // Only add the imageView if the user has taken a picture
        if (mBinaryName != null) {
            mImageView = new ImageView(getContext());
            mImageView.setId(QuestionWidget.newUniqueId());
            Display display =
                ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay();
            int screenWidth = display.getWidth();
            int screenHeight = display.getHeight();

            File f = new File(mInstanceFolder + File.separator + mBinaryName);

            if (f.exists()) {
                Bitmap bmp = FileUtils.getBitmapScaledToDisplay(f, screenHeight, screenWidth);
                if (bmp == null) {
                    mErrorTextView.setVisibility(View.VISIBLE);
                }
                mImageView.setImageBitmap(bmp);
            } else {
                mImageView.setImageBitmap(null);
            }

            mImageView.setPadding(10, 10, 10, 10);
            mImageView.setAdjustViewBounds(true);
            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   	Collect.getInstance().getActivityLogger().logInstanceAction(this, "viewButton", 
                			"click", mPrompt.getIndex());
                    Intent i = new Intent("android.intent.action.VIEW");
                    Uri uri = MediaUtils.getImageUriFromMediaProvider(mInstanceFolder + File.separator + mBinaryName);
                	if ( uri != null ) {
                        Log.i(t,"setting view path to: " + uri);
                        i.setDataAndType(uri, "image/*");
                        try {
                            getContext().startActivity(i);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getContext(),
                                getContext().getString(R.string.activity_not_found, "view image"),
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
            answerLayout.addView(mImageView);
        }
        addAnswerView(answerLayout);
    }


    private void deleteMedia() {
        // get the file path and delete the file
    	String name = mBinaryName;
        // clean up variables
    	mBinaryName = null;
    	// delete from media provider
        int del = MediaUtils.deleteImageFileFromMediaProvider(mInstanceFolder + File.separator + name);
        Log.i(t, "Deleted " + del + " rows from media content provider");
    }


    @Override
    public void clearAnswer() {
        // remove the file
        deleteMedia();
        mImageView.setImageBitmap(null);
        mErrorTextView.setVisibility(View.GONE);

        // reset buttons
        mCaptureButton.setText(getContext().getString(R.string.capture_image));
    }


    @Override
    public IAnswerData getAnswer() {
        if (mBinaryName != null) {
            return new StringData(mBinaryName.toString());
        } else {
            return null;
        }
    }


    @Override
    public void setBinaryData(Object newImageObj) {
        // you are replacing an answer. delete the previous image using the
        // content provider.
        if (mBinaryName != null) {
            deleteMedia();
        }

        File newImage = (File) newImageObj;
        if (newImage.exists()) {
            int rotationAmount=0;
            try {
                ExifInterface exif = new ExifInterface(newImage.getAbsolutePath());
                String exifOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
                int orientation=Integer.parseInt(exifOrientation);
                switch (orientation) {
                    case 1:
                        rotationAmount=0;
                        break;
                    case 3:
                        rotationAmount=180;
                        break;
                    case 6:
                        rotationAmount=90;
                        break;
                    case 8:
                        rotationAmount=270;
                        break;
                    default:
                        Log.e(t, "Invalid orientation!");
                }
            } catch (IOException iox) {
                Log.e(t, "Failed to get orientation!");
            }
            if(rotationAmount!=0) {
                rotateImage((File) newImageObj, newImage, rotationAmount);
            }

            // Add the new image to the Media content provider so that the
            // viewing is fast in Android 2.0+
        	ContentValues values = new ContentValues(6);
            values.put(Images.Media.TITLE, newImage.getName());
            values.put(Images.Media.DISPLAY_NAME, newImage.getName());
            values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
            values.put(Images.Media.MIME_TYPE, "image/jpeg");
            values.put(Images.Media.DATA, newImage.getAbsolutePath());

            Uri imageURI = getContext().getContentResolver().insert(
            		Images.Media.EXTERNAL_CONTENT_URI, values);
            Log.i(t, "Inserting image returned uri = " + imageURI.toString());

            mBinaryName = newImage.getName();
            Log.i(t, "Setting current answer to " + newImage.getName());
        } else {
            Log.e(t, "NO IMAGE EXISTS at: " + newImage.getAbsolutePath());
        }

    	Collect.getInstance().getFormController().setIndexWaitingForData(null);
    }

    private static void rotateImage(File newImageObj, File newImage, int rotationAmount) {
        try {
            Bitmap original = BitmapFactory.decodeFile(newImage.getAbsolutePath());
            int originalWidth=original.getWidth();
            int originalHeight=original.getHeight();
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationAmount);
            FileOutputStream fos = null;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, originalWidth, originalHeight, true);
            Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
            int newWidth=rotatedBitmap.getWidth();
            int newHeight=rotatedBitmap.getHeight();
            try {
                fos = new FileOutputStream(newImageObj.getAbsolutePath());
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            } catch (Exception ex) {
                Log.e("Saving bitmap", ex.getMessage());
            } finally {
                try {
                    if (fos != null)
                        fos.close();
                } catch (IOException iox) {
                    Log.e("Closing bitmap", iox.getMessage());
                }
            }
        }catch (Exception ex) {
            Log.e("Rotating bitmap", ex.getMessage());
        }
    }

    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        InputMethodManager inputManager =
            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getWindowToken(), 0);
    }


    @Override
    public boolean isWaitingForBinaryData() {
    	return mPrompt.getIndex().equals(Collect.getInstance().getFormController().getIndexWaitingForData());
    }


    @Override
	public void cancelWaitingForBinaryData() {
    	Collect.getInstance().getFormController().setIndexWaitingForData(null);
	}


    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mCaptureButton.setOnLongClickListener(l);
        mChooseButton.setOnLongClickListener(l);
        if (mImageView != null) {
            mImageView.setOnLongClickListener(l);
        }
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mCaptureButton.cancelLongPress();
        mChooseButton.cancelLongPress();
        if (mImageView != null) {
            mImageView.cancelLongPress();
        }
    }

}
