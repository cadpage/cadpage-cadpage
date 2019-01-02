// IContentService.aidl
package net.anei.cadpagesupport;

import net.anei.cadpagesupport.ContentCursor;

interface IContentService {

  ContentCursor query(String url, in String[] projection, String selection, in String[] selectArgs, String sortOrder);

}
