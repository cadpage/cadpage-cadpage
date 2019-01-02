package net.anei.cadpagesupport;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Parcel;
import android.os.Parcelable;

public class ContentCursor extends MatrixCursor implements Parcelable {

  public ContentCursor(Cursor cursor) {
    super(cursor.getColumnNames(), cursor.getCount());
    if (cursor.moveToFirst()) {
      do {
        addRow(getRowValues(cursor));
      } while (cursor.moveToNext()) ;
    }
  }

  private static Object[] getRowValues(Cursor cursor) {
    Object[] values = new Object[cursor.getColumnCount()];
    for (int col = 0; col < values.length; col++) {
      switch (cursor.getType(col)) {

        case Cursor.FIELD_TYPE_INTEGER:
          values[col] = cursor.getInt(col);
          break;

        case Cursor.FIELD_TYPE_FLOAT:
          values[col] = cursor.getFloat(col);
          break;

        case Cursor.FIELD_TYPE_STRING:
          values[col] = cursor.getString(col);
          break;

        case Cursor.FIELD_TYPE_BLOB:
          values[col] = cursor.getBlob(col);
          break;

        default:
          values[col] = null;
      }
    }
    return values;
  }

  public static final Creator<ContentCursor> CREATOR = new Creator<ContentCursor>() {
    @Override
    public ContentCursor createFromParcel(Parcel in) {
      return new ContentCursor(in);
    }

    @Override
    public ContentCursor[] newArray(int size) {
      return new ContentCursor[size];
    }
  };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeStringArray(getColumnNames());
    int recCountPos = dest.dataPosition();
    dest.writeInt(0);  // record count will be recorded later
    int recCount = 0;
    if (moveToFirst()) {
      do {
        recCount++;
        dest.writeArray(getRowValues(this));
      } while (moveToNext());
    }
    dest.setDataPosition(recCountPos);
    dest.writeInt(recCount);
  }

  protected ContentCursor(Parcel in) {
    super(in.createStringArray());
    int recCount = in.readInt();
    for (int row = 0; row < recCount; row++) {
      addRow(in.readArray(ContentCursor.class.getClassLoader()));
    }
  }
}
