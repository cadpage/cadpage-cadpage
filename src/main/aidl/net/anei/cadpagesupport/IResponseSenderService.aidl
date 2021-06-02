// IResponseSenderService.aidl
package net.anei.cadpagesupport;

interface IResponseSenderService {

  oneway void callPhone(in String phone);

  oneway void sendSMS(in String target, in String message);

  oneway void mmsDownload(in String content, in Uri downloadUri, in int subscriptionId, in PendingIntent pIntent);
}
